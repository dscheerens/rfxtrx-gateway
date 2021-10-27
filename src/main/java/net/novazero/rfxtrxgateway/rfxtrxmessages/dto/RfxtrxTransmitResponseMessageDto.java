package net.novazero.rfxtrxgateway.rfxtrxmessages.dto;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Introspected
public record RfxtrxTransmitResponseMessageDto(
    @NotBlank String subType,
    @Min(0) @Max(255) int sequenceNumber,
    @NotBlank String responseType
) implements RfxtrxMessageDto {
    public static final String TYPE = "TRANSMIT_RESPONSE";
}
