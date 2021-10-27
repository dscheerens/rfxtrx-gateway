package net.novazero.rfxtrxgateway;

public class RfxtrxServiceException extends RuntimeException {
    public RfxtrxServiceException() {
    }

    public RfxtrxServiceException(String message) {
        super(message);
    }

    public RfxtrxServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RfxtrxServiceException(Throwable cause) {
        super(cause);
    }
}
