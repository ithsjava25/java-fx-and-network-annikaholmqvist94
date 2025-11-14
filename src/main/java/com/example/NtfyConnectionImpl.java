package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


/**
 * Implementation of NtfyConnection using Java's built-in HttpClient for both sending messages and subscribing to a topic.
 * Manages the HTTP connection logic for text messages, file uploads.
 */
public class NtfyConnectionImpl implements NtfyConnection {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String hostName;
    private final ObjectMapper mapper = new ObjectMapper();


    private String currentTopic = "mytopic";

    private CompletableFuture<Void> currentSubscription = CompletableFuture.completedFuture(null);


    public NtfyConnectionImpl() {

        Dotenv dotenv = Dotenv.load();

        this.hostName = Objects.requireNonNull(dotenv.get("HOST_NAME"));
    }

    /**
     * Creates a new connection implementation.
     * hostName The base URL of the Ntfy server.
     */
    public NtfyConnectionImpl(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public String getTopic() {
        return currentTopic;
    }


    /**
     * Establishes a connection to the Ntfy topic to receive messages in real-time.
     * This method runs asynchronously in a dedicated thread.
     */
    @Override
    public void connect(String newTopic, Consumer<NtfyMessageDto> messageHandler) {

        currentSubscription.cancel(true);
        System.out.println("Cancelled subscription on " + currentTopic);


        currentTopic = newTopic;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(hostName + "/" + currentTopic + "/json"))
                .build();


        currentSubscription = http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body()
                        .map(s -> {
                            try {
                                return mapper.readValue(s, NtfyMessageDto.class);
                            } catch (JsonProcessingException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .filter(message -> message.event().equals("message"))
                        .forEach(messageHandler))
                .exceptionally(e -> {

                    if (!(e instanceof java.util.concurrent.CancellationException)) {
                        System.out.println("Failure in receiving on: " + currentTopic + ": " + e.getMessage());
                    }
                    return null;
                });
    }


    /**
     * Sends a text message to the specified Ntfy topic.
     * @param message The text content of the message.
     * @param topic The Ntfy topic to send the message to.
     */
    @Override
    public boolean send(String message, String topic) {

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .header("Cache", "no")
                .uri(URI.create(hostName + "/" + topic))
                .build();

        try {

            HttpResponse<Void> response = http.send(httpRequest, HttpResponse.BodyHandlers.discarding());

            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException e) {
            System.out.println("Error sending message to " + topic + ": " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted sending message to " + topic + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public void receive(Consumer<NtfyMessageDto> messageHandler) {
        // Återanvänder connect-logiken
        connect(currentTopic, messageHandler);
    }


    private String cleanHeaderValue(String value) {

        return value.replaceAll("[^\\w.\\-]", "-");
    }


    @Override
    public boolean sendFile(File file, String topic) {
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not valid: " + file.getAbsolutePath());
            return false;
        }

        try {

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }


            String cleanedFilename = cleanHeaderValue(file.getName());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                    .header("Content-Type", contentType)
                    .header("Filename", cleanedFilename) // Använder det rensade filnamnet
                    .uri(URI.create(hostName + "/" + topic))
                    .build();


            HttpResponse<Void> response = http.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (IOException e) {
            System.out.println("Could not transfer file or read file: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            System.out.println("Interrupted file transfer: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
