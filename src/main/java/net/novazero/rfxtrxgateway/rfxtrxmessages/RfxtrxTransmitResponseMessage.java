package net.novazero.rfxtrxgateway.rfxtrxmessages;

import java.util.Optional;

public class RfxtrxTransmitResponseMessage extends RfxtrxMessage {
    public static final byte PACKET_TYPE = 0x02;
    private static final byte PACKET_LENGTH = 0x04;

    public enum SubType {
        ERROR_RECEIVER_DID_NOT_LOCK((byte) 0),
        RESPONSE((byte) 1);

        public final byte value;

        SubType(byte value) {
            this.value = value;
        }

        public static Optional<SubType> from(byte value) {
            for (var subType : SubType.values()) {
                if (subType.value == value) {
                    return Optional.of(subType);
                }
            }

            return Optional.empty();
        }

        public static Optional<SubType> from(String value) {
            for (var subType : SubType.values()) {
                if (subType.toString().equals(value)) {
                    return Optional.of(subType);
                }
            }

            return Optional.empty();
        }
    }

    public enum ResponseType {
        ACK((byte) 0),
        ACK_DELAYED((byte) 1),
        NAK_NO_FREQUENCY_LOCK((byte) 2),
        NAK_INVALID_AC_ADDRESS((byte) 3);

        public final byte value;

        ResponseType(byte value) {
            this.value = value;
        }

        public static Optional<ResponseType> from(byte value) {
            for (var responseType : ResponseType.values()) {
                if (responseType.value == value) {
                    return Optional.of(responseType);
                }
            }

            return Optional.empty();
        }

        public static Optional<ResponseType> from(String value) {
            for (var responseType : ResponseType.values()) {
                if (responseType.toString().equals(value)) {
                    return Optional.of(responseType);
                }
            }

            return Optional.empty();
        }
    }

    public final SubType subType;
    public final int sequenceNumber;
    public final ResponseType responseType;

    public RfxtrxTransmitResponseMessage(SubType subType, int sequenceNumber, ResponseType responseType) {
        this.subType = subType;
        this.sequenceNumber = sequenceNumber;
        this.responseType = responseType;
    }

    @Override
    public byte[] serialize() {
        return new byte[] {
            PACKET_LENGTH,
            PACKET_TYPE,
            subType.value,
            (byte) sequenceNumber,
            responseType.value
        };
    }

    @Override
    public String toString() {
        return "RfxtrxTransmitResponseMessage{" +
            "subType=" + subType +
            ", sequenceNumber=" + sequenceNumber +
            ", responseType=" + responseType +
            '}';
    }

    public static Optional<RfxtrxTransmitResponseMessage> deserialize(byte[] packet) {
        if (packet.length != (PACKET_LENGTH + 1) || packet[0] != PACKET_LENGTH || packet[1] != PACKET_TYPE) {
            return Optional.empty();
        }

        var subType = SubType.from(packet[2]);
        var sequenceNumber = packet[3] & 0xFF;
        var responseType = ResponseType.from(packet[4]);

        if (subType.isEmpty() || responseType.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RfxtrxTransmitResponseMessage(
            subType.get(),
            sequenceNumber,
            responseType.get()
        ));
    }
}
