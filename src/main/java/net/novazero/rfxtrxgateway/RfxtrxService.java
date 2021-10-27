package net.novazero.rfxtrxgateway;

import com.fazecast.jSerialComm.SerialPort;
import io.micronaut.core.util.StringUtils;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.novazero.rfxtrxgateway.rfxtrxmessages.RfxtrxMessage;
import net.novazero.rfxtrxgateway.rfxtrxmessages.RfxtrxMessageDeserializer;
import net.novazero.rfxtrxgateway.utils.HexUtils;
import net.novazero.rfxtrxgateway.utils.RxLog;
import net.novazero.rfxtrxgateway.utils.rfxtrx.RfxtrxTranscodingUtils;
import net.novazero.rfxtrxgateway.utils.serialport.SerialPortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public class RfxtrxService {
    private static final Logger LOG = LoggerFactory.getLogger(RfxtrxService.class);

    private final RfxtrxConfiguration rfxtrxConfiguration;
    private final RfxtrxMessageDeserializer rfxtrxMessageDeserializer;

    private final PublishSubject<WriteMessageCommand> writeMessageCommandSubject = PublishSubject.create();
    private Flowable<RfxtrxMessage> _rfxtrxMessage$;
    private Flowable<SerialPort> _rfxtrxSerialPort$;
    private final PublishSubject<RfxtrxMessage> writtenMessageSubject = PublishSubject.create();

    public enum WriteResult {
        OK,
        NO_SERIAL_PORT_AVAILABLE,
        FAILED,
    }

    private static record WriteMessageCommand(
        RfxtrxMessage message,
        Subject<WriteResult> resultSubject
    ) {}

    private static record AllocatedWriteMessageCommand(
        RfxtrxMessage message,
        Subject<WriteResult> resultSubject,
        Optional<SerialPort> port
    ) {}

    @Inject
    public RfxtrxService(RfxtrxConfiguration rfxtrxConfiguration, RfxtrxMessageDeserializer rfxtrxMessageDeserializer) {
        this.rfxtrxConfiguration = rfxtrxConfiguration;
        this.rfxtrxMessageDeserializer = rfxtrxMessageDeserializer;
    }

    public Disposable connect() {
         var readSubscription = rfxtrxMessage$().subscribe();

         var singleThreadScheduler = Schedulers.from(Executors.newSingleThreadExecutor());

         var writeSubscription = writeMessageCommandSubject
             .toFlowable(BackpressureStrategy.BUFFER)
             .observeOn(singleThreadScheduler)
             .concatMap((command) ->
                 rfxtrxSerialPort$()
                     .timeout(1, TimeUnit.SECONDS, singleThreadScheduler)
                     .firstOrError()
                     .toFlowable()
                     .map(Optional::of)
                     .onErrorReturn((error) -> Optional.empty())
                     .map((port) -> new AllocatedWriteMessageCommand(command.message, command.resultSubject, port))
             )
             .compose(RxLog.debug(LOG, (command) -> "writing message: " + command.message.toString()))
             .subscribe((command) -> {
                 if (command.port.isEmpty()) {
                     command.resultSubject.onNext(WriteResult.NO_SERIAL_PORT_AVAILABLE);
                     command.resultSubject.onComplete();
                     return;
                 }

                 var packet = command.message.serialize();
                 var numberOfBytesWritten = command.port.get().writeBytes(packet, packet.length);
                 var wasSuccessful = numberOfBytesWritten == packet.length;

                 command.resultSubject.onNext(wasSuccessful ? WriteResult.OK : WriteResult.FAILED);
                 command.resultSubject.onComplete();

                 Schedulers.io().scheduleDirect(() -> writtenMessageSubject.onNext(command.message));
             });

        return Disposable.fromAction(() -> {
            readSubscription.dispose();
            writeSubscription.dispose();
            singleThreadScheduler.shutdown();
        });
    }

    public Flowable<RfxtrxMessage> rfxtrxMessage$() {
        if (_rfxtrxMessage$ == null) {
            var receivedMessage$ = rfxtrxSerialPort$()
                .compose(SerialPortUtils.readData())
                .compose(RxLog.debug(LOG, (data) -> "Received data chunk: " + HexUtils.asHexString(data) + " (" + data.length + " bytes)"))
                .compose(RfxtrxTranscodingUtils.extractRawRfxtrxPackets())
                .compose(RxLog.debug(LOG, (data) -> "Received packet: " + HexUtils.asHexString(data) + " (" + data.length + " bytes)"))
                .map(this.rfxtrxMessageDeserializer::deserialize)
                .compose(RxLog.debug(LOG, (message) -> message
                    .map(rfxtrxMessage -> "Received message: " + rfxtrxMessage)
                    .orElse("Unable to deserialize packet")
                ))
                .filter(Optional::isPresent)
                .map(Optional::get);

            var writtenMessage$ = writtenMessageSubject.toFlowable(BackpressureStrategy.BUFFER);

            _rfxtrxMessage$ = Flowable.merge(receivedMessage$, writtenMessage$).share();
        }
        return _rfxtrxMessage$;
    }

    public Flowable<WriteResult> writeMessage(RfxtrxMessage message) {
        var writeResultSubject = ReplaySubject.<WriteResult>create(1);
        var writeMessageCommand = new WriteMessageCommand(message, writeResultSubject);
        writeMessageCommandSubject.onNext(writeMessageCommand);

        return writeResultSubject.toFlowable(BackpressureStrategy.LATEST).observeOn(Schedulers.io());
    }

    private Flowable<SerialPort> rfxtrxSerialPort$() {
        if (_rfxtrxSerialPort$ == null) {
            var port$ = Flowable.<SerialPort>create(emitter -> {
                var rfxtrxSerialPort = findRfxtrxSerialPort();

                if (rfxtrxSerialPort.isEmpty()) {
                    emitter.onError(new RfxtrxServiceException("RFXtrx serial port not found"));
                    return;
                }

                emitter.onNext(rfxtrxSerialPort.get());
                emitter.onComplete();
            }, BackpressureStrategy.BUFFER);

            var openPort = SerialPortUtils.openPort(38400, 8, 1, SerialPort.NO_PARITY);

            _rfxtrxSerialPort$ = port$
                .compose(openPort)
                .map(Optional::of)
                .onErrorComplete()
                .concatWith(
                    port$
                        .delay(5500, TimeUnit.MILLISECONDS, Schedulers.io())
                        .compose(openPort)
                        .map(Optional::of)
                        .startWith(Flowable.just(Optional.empty()))
                        .onErrorComplete()
                        .repeatWhen((x) -> x.delay(5, TimeUnit.SECONDS, Schedulers.io()))
                )
                .replay(1)
                .refCount()
                .filter(Optional::isPresent)
                .map(Optional::get);
        }

        return _rfxtrxSerialPort$;
    }

    private Optional<SerialPort> findRfxtrxSerialPort() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for RFXtrx serial port...");
        }

        var serialPorts = SerialPort.getCommPorts();
        var targetSerialPortName = rfxtrxConfiguration.getSerialPortName();
        var autoDetect = StringUtils.isEmpty(targetSerialPortName) || targetSerialPortName.equalsIgnoreCase(RfxtrxConfiguration.AUTO_SELECT_SERIAL_PORT);

        var selectedSerialPort = Arrays.stream(serialPorts)
            .filter((serialPort) -> autoDetect
                ? serialPort.getPortDescription().toLowerCase().contains("rfxtrx")
                : serialPort.getSystemPortName().equals(targetSerialPortName)
            )
            .findFirst();

        if (LOG.isDebugEnabled()) {
            if (autoDetect) {
                selectedSerialPort.ifPresentOrElse(
                    (port) -> LOG.debug("Automatically selected " + port.getSystemPortName()),
                    () -> LOG.debug("Failed to automatically select RFXtrx serial port: no port was found")
                );
            } else {
                selectedSerialPort.ifPresentOrElse(
                    (port) -> LOG.debug("Found configured port " + port.getSystemPortName()),
                    () -> LOG.debug("Failed to find configured port " + targetSerialPortName)
                );
            }
        }

        return selectedSerialPort;
    }
}
