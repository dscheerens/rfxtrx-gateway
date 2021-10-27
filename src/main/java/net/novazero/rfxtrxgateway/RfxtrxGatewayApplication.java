package net.novazero.rfxtrxgateway;

import io.micronaut.runtime.Micronaut;

public class RfxtrxGatewayApplication {
    public static void main(String[] arguments) {
        var applicationContext = Micronaut.run(RfxtrxGatewayApplication.class, arguments);

        applicationContext
            .findBean(RfxtrxService.class)
            .ifPresent(RfxtrxService::connect);
    }
}
