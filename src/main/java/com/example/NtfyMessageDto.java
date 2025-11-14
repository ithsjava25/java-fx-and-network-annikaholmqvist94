package com.example;


import java.time.Instant;

public record NtfyMessageDto(String id, long time, String event, String topic, String type, String message, boolean isLocal) {

    // Sekundär konstruktor för mottagna meddelanden (isLocal = false)
    public NtfyMessageDto(String id, long time, String event, String topic, String type, String message) {
        this(id, time, event, topic, type, message, false);
    }

    /**
     * Konstruktor för att skapa ett lokalt skickat meddelande (som visas i listan innan bekräftelse).
     * @param message Den huvudsakliga meddelandetexten eller filnamnet.
     * @param topic Den aktuella topicen.
     * @param type Typ av innehåll ("text" eller "file").
     * @param isLocal Alltid true för lokalt skickade meddelanden.
     */
    public NtfyMessageDto(String message, String topic, String type, boolean isLocal) {

        this(null, Instant.now().getEpochSecond(), "message", topic, type, message, isLocal);
    }

    // Kort konstruktor för att bara skicka en meddelandetext (kanske inte används men fixas för konsekvens)
    public NtfyMessageDto(String message) {
        this(null, 0L, "message", null, "text", message, true);
    }

    @Override
    public String toString(){
        return message;
    }

}

