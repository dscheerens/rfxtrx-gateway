# RFXtrx gateway server

This project contains a microservice that acts as a gateway for [RFXtrx devices](http://www.rfxcom.com/).
The gateway allows for streaming messages (read / write) using a websocket interface (`/ws/rfxtrx`).
Commands can also be written using a rest endpoint (`POST /api/rfxtrx`).

## Tech-stack

* [Java 17](https://www.oracle.com/java/)
* [Micronaut 3](https://micronaut.io/)
* [RxJava 3](https://github.com/ReactiveX/RxJava)
* [jSerialComm](https://github.com/Fazecast/jSerialComm)

## Supported messages

Currently the server only recognizes a small subset of the RFXtrx messages ("lighting2" and transmit responses).
Since I couldn't find any documentation regarding the RFXtrx messages, all (de)serialization is based on the following open source projects:
* [node-rfxcom](https://github.com/rfxcom/node-rfxcom)
* [openHAB RFXCOM addon](https://github.com/openhab/openhab-addons/tree/main/bundles/org.openhab.binding.rfxcom)

Use the following steps to add support for extra messages:
1. Create the message class in the `net.novazero.rfxtrxgateway.rfxtrxmessages` package.
   The new message class should extend the `RfxtrxMessage` class.
2. Add the message class to the deserializer map in the `RfxtrxMessageDeserializer` class.
3. Create the DTO class for the message in the `net.novazero.rfxtrxgateway.rfxtrxmessages.dto` package.
   It should implement the `RfxtrxMessageDto` interface.
4. Add `toDto` and `fromDto` overload functions for the new message type in the `RfxtrxMessageConverter` class.
5. Extend the generic `toDto` and `fromDto` functions in the `RfxtrxMessageConverter` class.
6. Extend the `@JsonSubTypes` annotation of the `RfxtrxMessageDto` interface (so [Jackson](https://github.com/FasterXML/jackson) knows how to handle the polymorphic type).