package com.example;

import java.io.File;
import java.util.function.Consumer;

public class NtfyConnectionSpy implements NtfyConnection {
    private String lastSentMessage = null;
    private String lastSentTopic = null;
    private File lastSentFile = null;
    private boolean connectCalled = false;
    Consumer<NtfyMessageDto> messageHandler = null;
    private String lastConnectTopic = null;

    /**
     * Återställer spionens tillstånd.
     */
    public void reset() {
        lastSentMessage = null;
        lastSentTopic = null;
        lastSentFile = null;
        connectCalled = false;
        messageHandler = null;
        lastConnectTopic = null;
    }

    // --- Implementering av NtfyConnection Interface ---

    @Override
    public String getTopic() {
        // En spion kan returnera den senaste anslutna topicen, eller en tom sträng om inte relevant
        return lastConnectTopic != null ? lastConnectTopic : "";
    }

    @Override
    public void connect(String topic, Consumer<NtfyMessageDto> messageHandler) {
        this.connectCalled = true;
        this.lastConnectTopic = topic;
        this.messageHandler = messageHandler;
    }

    @Override
    public void receive(Consumer<NtfyMessageDto> messageHandler) {

    }


    @Override
    public boolean send(String message, String topic) {
        // SPIONLOGIK: Spara det skickade meddelandet och ämnet
        this.lastSentMessage = message;
        this.lastSentTopic = topic;
        return true; // Simulerar framgångsrikt skickande
    }

    @Override
    public boolean sendFile(File file, String topic) { // RETURTYPEN ÄNDRAD TILL BOOLEAN
        // SPIONLOGIK: Spara den skickade filen och ämnet
        this.lastSentFile = file;
        this.lastSentTopic = topic;
        return true; // Simulerar framgångsrikt skickande
    }

    // --- Metoder för att simulera mottagande av meddelanden (för testning) ---

    /**
     * Simulerar att ett meddelande tas emot från servern och anropar
     * den ursprungliga messageHandler.
     * @param messageDto Meddelandet som ska simuleras att tas emot.
     */
    public void simulateMessageReceived(NtfyMessageDto messageDto) {
        if (messageHandler != null) {
            messageHandler.accept(messageDto);
        }
    }

    // --- Getters för testverifieringar Topic behålls för flexibilitet att skapa fler chattrum---

    public String getLastSentMessage() {
        return lastSentMessage;
    }

    public String getLastSentTopic() {
        return lastSentTopic;
    }

    public File getLastSentFile() {
        return lastSentFile;
    }

    public boolean isConnectCalled() {
        return connectCalled;
    }

    public String getLastConnectTopic() {
        return lastConnectTopic;
    }

    /**
     * Används för att kontrollera att en messageHandler sattes.
     */
    public boolean hasMessageHandler() {
        return messageHandler != null;
    }
}