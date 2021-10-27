package net.novazero.rfxtrxgateway.utils.serialport;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class SerialPortUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SerialPortUtils.class);

    private SerialPortUtils() {}

    public static FlowableTransformer<SerialPort, SerialPort> openPort(int baudRate, int dataBits, int stopBits, int parity) {
        return (port$) -> port$.switchMap((port) -> Flowable.<SerialPort>create(emitter -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Opening serial port " + port.getSystemPortName() + "...");
            }

            if (!port.isOpen() && !port.openPort()) {
                var errorMessage = "Failed to open serial port " + port.getSystemPortName() +", openPort() returned false";
                if (LOG.isDebugEnabled()) {
                    LOG.debug(errorMessage);
                }
                emitter.onError(new SerialPortException(errorMessage));
                return;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting serial port " + port.getSystemPortName() + " parameters (" + baudRate + "," + dataBits + "," + stopBits + "," + parity +")...");
            }

            if (!port.setComPortParameters(baudRate, dataBits, stopBits, parity)) {
                var errorMessage = "Failed to set serial port " + port.getSystemPortName() + " parameters (" + baudRate + "," + dataBits + "," + stopBits + "," + parity +")";
                if (LOG.isDebugEnabled()) {
                    LOG.debug(errorMessage);
                }
                emitter.onError(new SerialPortException(errorMessage));
                if (port.isOpen()) {
                    port.closePort();
                }
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("Opened serial port " + port.getSystemPortName());
            }

            var disconnectDetectionSubscription = Flowable.interval(1, TimeUnit.SECONDS)
                .map((x) -> port.isOpen())
                .skipWhile((portIsOpen) -> portIsOpen)
                .take(1)
                .subscribe((x) -> {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Serial port " + port.getSystemPortName() + " was closed");
                    }

                    emitter.onComplete();
                });

            emitter.setCancellable(() -> {
                disconnectDetectionSubscription.dispose();

                if (port.isOpen()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Closing serial port " + port.getSystemPortName() + "...");
                    }
                    port.closePort();
                }
            });

            emitter.onNext(port);
        }, BackpressureStrategy.LATEST));
    }

    public static FlowableTransformer<SerialPort, byte[]> readData() {
        return (port$) -> port$.switchMap((port) -> Flowable.<byte[]>create(emitter -> {
            var dataListenerAdded = port.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (emitter.isCancelled() || event.getEventType() != SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                        return;
                    }

                    emitter.onNext(event.getReceivedData());
                }
            });

            if (!dataListenerAdded) {
                var errorMessage = "Failed to add data listener for serial port " + port.getSystemPortName();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(errorMessage);
                }
                emitter.onError(new SerialPortException(errorMessage));
                return;
            }

            emitter.setCancellable(port::removeDataListener);
        }, BackpressureStrategy.BUFFER));
    }
}
