package net.novazero.rfxtrxgateway.rfxtrxmessages;

import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Singleton
public class RfxtrxMessageDeserializer {
    private final Map<Byte, Function<byte[], Optional<? extends  RfxtrxMessage>>> deserializers;

    public RfxtrxMessageDeserializer() {
        this(new HashMap<>() {{
            put(RfxtrxTransmitResponseMessage.PACKET_TYPE, RfxtrxTransmitResponseMessage::deserialize);
            put(RfxtrxLighting2Message.PACKET_TYPE, RfxtrxLighting2Message::deserialize);
        }});
    }

    public RfxtrxMessageDeserializer(Map<Byte, Function<byte[], Optional<? extends  RfxtrxMessage>>> deserializers) {
        this.deserializers = deserializers;
    }

    public Optional<RfxtrxMessage> deserialize(byte[] packet) {
        if (packet.length < 2) {
            return Optional.empty();
        }

        return Optional.ofNullable(deserializers.get(packet[1])).flatMap((deserializer) -> deserializer.apply(packet));
    }
}
