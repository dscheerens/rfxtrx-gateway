package net.novazero.rfxtrxgateway.rfxtrxmessages.dto;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Introspected
public record RfxtrxLighting2MessageDto(
    @NotBlank String subType,
    @Min(0) @Max(255) int sequenceNumber,
    @NotBlank String transmitterId,
    byte unitCode,
    @NotBlank String commandType,
    @Min(0) @Max(15) byte dimmingLevel,
    @Min(0) @Max(15) byte rssi
) implements RfxtrxMessageDto {
    public static final String TYPE = "LIGHTING2";
}
