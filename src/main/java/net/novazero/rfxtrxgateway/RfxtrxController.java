package net.novazero.rfxtrxgateway;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.codec.CodecException;
import io.micronaut.jackson.codec.JsonMediaTypeCodec;
import io.micronaut.validation.Validated;
import io.micronaut.validation.validator.Validator;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jakarta.inject.Inject;
import net.novazero.rfxtrxgateway.rfxtrxmessages.dto.RfxtrxMessageDto;
import net.novazero.rfxtrxgateway.rfxtrxmessages.dto.RfxtrxMessageConverter;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ServerWebSocket("/ws/rfxtrx")
@Controller("api/rfxtrx")
@Validated
public class RfxtrxController {
    private static final Logger LOG = LoggerFactory.getLogger(RfxtrxController.class);

    private final RfxtrxService rfxtrxService;
    private final RfxtrxMessageConverter rfxtrxMessageConverter;
    private final JsonMediaTypeCodec jsonMediaTypeCodec;
    private final Map<String, PublishSubject<Boolean>> sessionClosedSubjects = new HashMap<>();
    private final Validator validator;

    @Inject
    public RfxtrxController(
        RfxtrxService rfxtrxService,
        RfxtrxMessageConverter rfxtrxMessageConverter,
        JsonMediaTypeCodec jsonMediaTypeCodec,
        Validator validator
    ) {
        this.rfxtrxService = rfxtrxService;
        this.rfxtrxMessageConverter = rfxtrxMessageConverter;
        this.jsonMediaTypeCodec = jsonMediaTypeCodec;
        this.validator = validator;
    }

    @OnOpen
    public Publisher<RfxtrxMessageDto> onOpen(WebSocketSession session) {
        var closeSubject = PublishSubject.<Boolean>create();
        sessionClosedSubjects.put(session.getId(), closeSubject);

        return rfxtrxService.rfxtrxMessage$()
            .map(rfxtrxMessageConverter::toDto)
            .filter(Optional::isPresent)
            .takeUntil(closeSubject.toFlowable(BackpressureStrategy.LATEST))
            .flatMap((message) -> session.send(message.get()));
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        var closeSubject = sessionClosedSubjects.get(session.getId());
        if (closeSubject != null) {
            sessionClosedSubjects.remove(session.getId());
            closeSubject.onNext(true);
            closeSubject.onComplete();
        }
    }

    @OnMessage
    public void onMessage(String rawMessage) {
        try {
            var messageDto = jsonMediaTypeCodec.decode(RfxtrxMessageDto.class, rawMessage);

            writeRfxtrxMessage(messageDto);
        } catch (CodecException codecException) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received unknown message: " + rawMessage, codecException);
            }
        }
    }

    @Post()
    public Publisher<? extends HttpResponse<Object>> writeMessage(@Valid RfxtrxMessageDto messageDto) {
        return rfxtrxMessageConverter.fromDto(messageDto)
            .map((message) -> rfxtrxService.writeMessage(message)
                .map((result) -> switch(result) {
                    case OK -> HttpResponse.ok();
                    case NO_SERIAL_PORT_AVAILABLE -> HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE);
                    case FAILED -> HttpResponse.serverError();
                }))
            .orElse(Flowable.just(HttpResponse.badRequest()));
    }

    private void writeRfxtrxMessage(RfxtrxMessageDto messageDto) {
        var violations = this.validator.validate(messageDto);
        if (violations.size() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Validation failed for: " + messageDto);
            }
            return;
        }

        var message = rfxtrxMessageConverter.fromDto(messageDto);

        message.ifPresent(rfxtrxService::writeMessage);
    }
}
