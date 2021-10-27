package net.novazero.rfxtrxgateway;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;

@ConfigurationProperties("rfxtrx")
public interface RfxtrxConfiguration {
    String AUTO_SELECT_SERIAL_PORT = "auto";

    @Bindable(defaultValue = AUTO_SELECT_SERIAL_PORT)
    String getSerialPortName();
}
