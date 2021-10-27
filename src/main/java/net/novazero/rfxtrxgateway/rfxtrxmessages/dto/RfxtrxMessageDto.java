package net.novazero.rfxtrxgateway.rfxtrxmessages.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RfxtrxTransmitResponseMessageDto.class, name = RfxtrxTransmitResponseMessageDto.TYPE),
    @JsonSubTypes.Type(value = RfxtrxLighting2MessageDto.class, name = RfxtrxLighting2MessageDto.TYPE)
})
public interface RfxtrxMessageDto { }
