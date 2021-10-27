package net.novazero.rfxtrxgateway.utils.serialport;

public class SerialPortException extends RuntimeException {
    public SerialPortException() {
    }

    public SerialPortException(String message) {
        super(message);
    }

    public SerialPortException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerialPortException(Throwable cause) {
        super(cause);
    }
}
