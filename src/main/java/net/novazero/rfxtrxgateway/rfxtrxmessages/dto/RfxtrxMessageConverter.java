package net.novazero.rfxtrxgateway.rfxtrxmessages.dto;

import jakarta.inject.Singleton;
import net.novazero.rfxtrxgateway.rfxtrxmessages.RfxtrxTransmitResponseMessage;
import net.novazero.rfxtrxgateway.utils.HexUtils;
import net.novazero.rfxtrxgateway.rfxtrxmessages.RfxtrxLighting2Message;
import net.novazero.rfxtrxgateway.rfxtrxmessages.RfxtrxMessage;

import java.util.Optional;

@Singleton
public class RfxtrxMessageConverter {
    public Optional<RfxtrxMessageDto> toDto(RfxtrxMessage message) {
        if (message instanceof RfxtrxLighting2Message m) {
            return Optional.of(toDto(m));
        }
        if (message instanceof RfxtrxTransmitResponseMessage m) {
            return Optional.of(toDto(m));
        }
        return Optional.empty();
    }

    public Optional<RfxtrxMessage> fromDto(RfxtrxMessageDto messageDto) {
        if (messageDto instanceof RfxtrxLighting2MessageDto m) {
            return fromDto(m).map(x -> x);
        }
        if (messageDto instanceof RfxtrxTransmitResponseMessageDto m) {
            return fromDto(m).map(x -> x);
        }
        return Optional.empty();
    }

    public RfxtrxTransmitResponseMessageDto toDto(RfxtrxTransmitResponseMessage message) {
        return new RfxtrxTransmitResponseMessageDto(
            message.subType.toString(),
            message.sequenceNumber,
            message.responseType.toString()
        );
    }

    public RfxtrxLighting2MessageDto toDto(RfxtrxLighting2Message message) {
        return new RfxtrxLighting2MessageDto(
            message.subType.toString(),
            message.sequenceNumber,
            String.format("%08x", message.transmitterId).toUpperCase(),
            message.unitCode,
            message.commandType.toString(),
            message.dimmingLevel,
            message.rssi
        );
    }

    public Optional<RfxtrxTransmitResponseMessage> fromDto(RfxtrxTransmitResponseMessageDto message) {
        var subType = RfxtrxTransmitResponseMessage.SubType.from(message.subType());
        var responseType = RfxtrxTransmitResponseMessage.ResponseType.from(message.responseType());

        if (subType.isEmpty() || responseType.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RfxtrxTransmitResponseMessage(
            subType.get(),
            message.sequenceNumber(),
            responseType.get()
        ));
    }

    public Optional<RfxtrxLighting2Message> fromDto(RfxtrxLighting2MessageDto message) {
        var subType = RfxtrxLighting2Message.SubType.from(message.subType());
        var transmitterId = HexUtils.parseInt(message.transmitterId());
        var commandType = RfxtrxLighting2Message.CommandType.from(message.commandType());

        if (subType.isEmpty() || commandType.isEmpty() || transmitterId.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RfxtrxLighting2Message(
            subType.get(),
            message.sequenceNumber(),
            transmitterId.get(),
            message.unitCode(),
            commandType.get(),
            message.dimmingLevel(),
            message.rssi()
        ));
    }
}
