package com.example;

import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


class HelloModelTest {

    private static final String DEFAULT_TOPIC = "mytopic";

    private void waitForFxUpdate() {
        try {
            // Vänta 100 millisekunder för att tillåta FX-tråden (om den finns) att köra
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // I HelloModelTest.java
    @Test
    @DisplayName("Given a model with messageToSend when calling sendMessage then send method on connection is called")
    void sendMessageCallsConnectionWithMsgToSend() {
        // Arrange - Given
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        String expectedMessage = "Hello World";
        model.setMessageToSend(expectedMessage);

        // Act - When
        model.sendMessage();

        waitForFxUpdate();

        // Assert - Then
        // 1. Verifiera att anslutningen anropades med rätt meddelande:
        assertThat(spy.getLastSentMessage()).isEqualTo(expectedMessage);

        // 2. Verifiera att det skickade meddelandet lades till i modellens lista (lokal uppdatering):
        assertThat(model.getMessages()).hasSize(1);
        assertThat(model.getMessages().get(0).message()).isEqualTo(expectedMessage);
        // Verifiera även att det markerades som lokalt skickat
        assertThat(model.getMessages().get(0).isLocal()).isTrue();
    }





    @Test
    void receiveMessageIsAddedToObservableList() {
        //Arrange  Given
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        var testMessage = "This is a test message";

        var testDto = new NtfyMessageDto(
                "id-123",                                // 1. id
                System.currentTimeMillis(),              // 2. time (Long)
                "message",                               // 3. event (Standard ntfy event type)
                DEFAULT_TOPIC,                           // 4. topic
                "text",                                  // 5. type ("text" för standardmeddelande)
                testMessage,                             // 6. message
                false                                    // 7. isLocal (mottaget meddelande är inte lokalt)
        );

        assertThat(model.getMessages()).isEmpty();

        //Act  When
        spy.messageHandler.accept(testDto);
        //Assert   Then
        assertThat(model.getMessages()).hasSize(1);
        // KORRIGERING: Använder NtfyMessageDto.message()
        assertThat(model.getMessages().get(0).message()).isEqualTo(testMessage);
    }



}