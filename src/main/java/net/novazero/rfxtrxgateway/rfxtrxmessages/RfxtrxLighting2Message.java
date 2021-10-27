package net.novazero.rfxtrxgateway.rfxtrxmessages;

import java.util.Optional;

public class RfxtrxLighting2Message extends RfxtrxMessage {
    public static final byte PACKET_TYPE = 0x11;
    private static final byte PACKET_LENGTH = 0x0B;

    public enum SubType {
        AC((byte) 0),
        HOME_EASY_EU((byte) 1),
        ANSLUT((byte) 2),
        KAMBROOK((byte) 3);

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

    public enum CommandType {
        OFF((byte) 0),
        ON((byte) 1),
        SET_LEVEL((byte) 2),
        GROUP_OFF((byte) 3),
        GROUP_ON((byte) 4),
        SET_GROUP_LEVEL((byte) 5);

        public final byte value;

        CommandType(byte value) {
            this.value = value;
        }

        public static Optional<CommandType> from(byte value) {
            for (var commandType : CommandType.values()) {
                if (commandType.value == value) {
                    return Optional.of(commandType);
                }
            }

            return Optional.empty();
        }

        public static Optional<CommandType> from(String value) {
            for (var commandType : CommandType.values()) {
                if (commandType.toString().equals(value)) {
                    return Optional.of(commandType);
                }
            }

            return Optional.empty();
        }
    }

    public final SubType subType;
    public final int sequenceNumber;
    public final int transmitterId;
    public final byte unitCode;
    public final CommandType commandType;
    public final byte dimmingLevel;
    public final byte rssi;

    public RfxtrxLighting2Message(SubType subType, int sequenceNumber, int transmitterId, byte unitCode, CommandType commandType, byte dimmingLevel, byte rssi) {
        this.subType = subType;
        this.sequenceNumber = sequenceNumber;
        this.transmitterId = transmitterId;
        this.unitCode = unitCode;
        this.commandType = commandType;
        this.dimmingLevel = dimmingLevel;
        this.rssi = rssi;
    }

    @Override
    public byte[] serialize() {
        return new byte[] {
            PACKET_LENGTH,
            PACKET_TYPE,
            subType.value,
            (byte) sequenceNumber,
            (byte) ((transmitterId >> 24) & 0xFF),
            (byte) ((transmitterId >> 16) & 0xFF),
            (byte) ((transmitterId >> 8) & 0xFF),
            (byte) (transmitterId & 0xFF),
            unitCode,
            commandType.value,
            dimmingLevel,
            (byte) (rssi << 4),
        };
    }

    @Override
    public String toString() {
        return "RfxtrxLighting2Message{" +
            "subType=" + subType +
            ", sequenceNumber=" + sequenceNumber +
            ", transmitterId=" + String.format("%08x", transmitterId).toUpperCase() +
            ", unitCode=" + unitCode +
            ", commandType=" + commandType +
            ", dimmingLevel=" + dimmingLevel +
            ", rssi=" + rssi +
            '}';
    }

    public static Optional<RfxtrxLighting2Message> deserialize(byte[] packet) {
        if (packet.length != (PACKET_LENGTH + 1) || packet[0] != PACKET_LENGTH || packet[1] != PACKET_TYPE) {
            return Optional.empty();
        }

        var subType = SubType.from(packet[2]);
        var sequenceNumber = packet[3] & 0xFF;
        var transmitterId = (packet[4] & 0xFF) << 24 | (packet[5] & 0xFF) << 16 | (packet[6] & 0xFF) << 8 | (packet[7] & 0xFF);
        var unitCode = packet[8];
        var commandType = CommandType.from(packet[9]);
        var dimmingLevel = packet[10];
        var rssi = (byte) ((packet[11] & 0xF0) >> 4);

        if (subType.isEmpty() || commandType.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RfxtrxLighting2Message(
            subType.get(),
            sequenceNumber,
            transmitterId,
            unitCode,
            commandType.get(),
            dimmingLevel,
            rssi
        ));
    }
}
