package net.novazero.rfxtrxgateway.utils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class HexUtils {
    private static final byte[] HEX = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private HexUtils() { }

    public static String asHexString(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        int index = 0;
        for (byte b : bytes) {
            hexChars[index++] = HEX[(0xF0 & b) >>> 4];
            hexChars[index++] = HEX[(0x0F & b)];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
