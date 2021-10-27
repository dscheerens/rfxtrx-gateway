package net.novazero.rfxtrxgateway.utils.rfxtrx;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;

public final class RfxtrxTranscodingUtils {
    private RfxtrxTranscodingUtils() { }

    public static FlowableTransformer<byte[], byte[]> extractRawRfxtrxPackets() {
        return (dataChunk$) -> Flowable.create(emitter -> {
            final var packetExtractor = new RfxtrxPacketExtractor();

            final var sourceSubscription = dataChunk$.subscribe(
                (dataChunk) -> {
                    for (var packet : packetExtractor.processChunk(dataChunk)) {
                        emitter.onNext(packet);
                    }
                },
                emitter::onError,
                emitter::onComplete
            );

            emitter.setCancellable(sourceSubscription::dispose);
        }, BackpressureStrategy.BUFFER);
    }
}
