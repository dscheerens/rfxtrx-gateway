package net.novazero.rfxtrxgateway.utils.rfxtrx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.LinkedList;

class RfxtrxPacketExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(RfxtrxPacketExtractor.class);

    private int requiredBytes = 0;
    private long lastChunkReceived = 0;
    private final ArrayDeque<Byte> buffer = new ArrayDeque<>();
    private final int incompletePacketTimeout;

    public RfxtrxPacketExtractor() {
        this(100);
    }

    public RfxtrxPacketExtractor(int incompletePacketTimeout) {
        this.incompletePacketTimeout = incompletePacketTimeout;
    }

    public Iterable<byte[]> processChunk(byte[] dataChunk) {
        var result = new LinkedList<byte[]>();

        var currentTime = System.currentTimeMillis();

        if (buffer.size() > 0 && currentTime - lastChunkReceived > incompletePacketTimeout) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Stale packet data detected, purging buffer...");
            }
            requiredBytes = 0;
            buffer.clear();
        }

        lastChunkReceived = currentTime;

        for (byte b : dataChunk) {
            buffer.addLast(b);
        }

        while (buffer.size() > 0 && buffer.size() >= requiredBytes) {
            if (requiredBytes > 0) {
                result.add(popArray(buffer, requiredBytes));
            }

            if (buffer.size() > 0 && (((int) buffer.peek()) & 0xFF) >= 4) {
                requiredBytes = (((int) buffer.peek()) & 0xFF) + 1;
            } else {
                requiredBytes = 0;
                buffer.clear();
            }
        }

        return result;
    }

    private static byte[] popArray(ArrayDeque<Byte> bytes, int length) {
        final var result = new byte[length];

        for (int index = 0; index < length; index++) {
            result[index] = bytes.pop();
        }

        return result;
    }
}
