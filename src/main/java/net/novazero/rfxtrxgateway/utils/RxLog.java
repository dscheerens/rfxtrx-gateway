package net.novazero.rfxtrxgateway.utils;

import io.reactivex.rxjava3.core.FlowableTransformer;
import org.slf4j.Logger;

import java.util.function.Function;

public final class RxLog {
    private RxLog() { }

    public static <T> FlowableTransformer<T, T> trace(Logger logger, Function<T, String> messageGenerator) {
        return (source$) -> source$.doOnNext((value) -> {
            if (logger.isTraceEnabled()) {
                logger.trace(messageGenerator.apply(value));
            }
        });
    }

    public static <T> FlowableTransformer<T, T> debug(Logger logger, Function<T, String> messageGenerator) {
        return (source$) -> source$.doOnNext((value) -> {
           if (logger.isDebugEnabled()) {
               logger.debug(messageGenerator.apply(value));
           }
        });
    }

    public static <T> FlowableTransformer<T, T> info(Logger logger, Function<T, String> messageGenerator) {
        return (source$) -> source$.doOnNext((value) -> {
            if (logger.isInfoEnabled()) {
                logger.info(messageGenerator.apply(value));
            }
        });
    }
}
