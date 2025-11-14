package com.example;

import javafx.beans.binding.Bindings;

import javafx.fxml.FXML;

import javafx.scene.control.*;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;



/**
 * Controller layer: Manages the user interface and mediates communication between the View (FXML) and the Model (HelloModel).
 * Handles user interactions such as sending messages, attaching files, and displaying chat content.
 */
public class HelloController {


    private static final String HOST_NAME = System.getenv("HOST_NAME");

    static {
        if (HOST_NAME == null || HOST_NAME.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable HOST_NAME must be set to the server URL."
            );
        }
    }





    /**
     * The model instance that holds application data and business logic.
     * Initializes connection to the specified Ntfy server.
     */
    private final HelloModel model = new HelloModel(new NtfyConnectionImpl(HOST_NAME));


    /**
     * The ListView component displaying the list of chat messages.
     */
    @FXML
    public ListView<NtfyMessageDto> chatListView;

    /**
     * The label displaying the current fixed topic being used.
     */
    @FXML
    public Label topicLabel;

    /**
     * The label indicating the name of the file currently attached for sending.
     */
    @FXML
    public Label attachedFileLabel;


    /**
     * The button used to send the message or the attached file.
     */
    @FXML
    private Button sendButton;

    @FXML
    private TextField messageInput;

    @FXML
    private Button attachFile;


    /**
     * Initializes the controller. This method is called automatically by the FXML loader.
     * It sets up bindings between the view components and the model and configures the chat list view.
     */
    @FXML
    private void initialize() {
        if (sendButton != null) {
            sendButton.setText(model.getGreeting());

            // Bindning: Knappen är inaktiverad ENDAST om BÅDE meddelandet är tomt OCH fil inte är bifogad
            sendButton.disableProperty().bind(
                    Bindings.createBooleanBinding(() -> {
                                boolean isMessageEmpty = model.messageToSendProperty().get() == null ||
                                        model.messageToSendProperty().get().trim().isEmpty();
                                boolean isFileNotAttached = model.fileToSendProperty().get() == null;

                                return isMessageEmpty && isFileNotAttached;
                            },
                            model.messageToSendProperty(),
                            model.fileToSendProperty())
            );
        }

        if (topicLabel != null) {
            // Visar den fasta topicen
            topicLabel.textProperty().bind(
                    Bindings.concat("Fixed Topic: ", model.currentTopicProperty())
            );
        }

        // Hanterar visning av bifogad fil
        if (attachedFileLabel != null) {
            model.fileToSendProperty().addListener((obs, oldFile, newFile) -> {
                if (newFile != null) {
                    attachedFileLabel.setText("Attached file: " + newFile.getName());
                    attachedFileLabel.setStyle("-fx-font-style: italic;" +
                            " -fx-font-size: 12px;" +
                            " -fx-text-fill: #008000;");
                } else {
                    attachedFileLabel.setText("No file attached");
                    attachedFileLabel.setStyle("-fx-font-style: italic; " +
                            "-fx-font-size: 12px; " +
                            "-fx-text-fill: #333;");
                }
            });
            attachedFileLabel.setText("No file attached");
        }

        if (messageInput!=null){
            messageInput.textProperty().bindBidirectional(model.messageToSendProperty());
        }

        if(chatListView!=null){
            chatListView.setItems(model.getMessages());
            // Använd den enkla CellFactoryn
            chatListView.setCellFactory(param -> new SimpleMessageCell());
        }
    }

    /**
     * A simple custom ListCell implementation for the chatListView.
     * It displays the message text or a placeholder for file uploads, and indicates if the message was sent locally.
     */
    private static class SimpleMessageCell extends ListCell<NtfyMessageDto> {

        @Override
        protected void updateItem(NtfyMessageDto item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle(null);
            } else {
                // Hämta meddelandet eller visa filstatus om meddelandet är tomt
                String displayMessage = item.message() != null && !item.message().trim().isEmpty()
                        ? item.message()
                        : ("file".equals(item.event())) ? item.topic() + " Uploaded" : "";

                // Lägg till prefix för att visa om det är skickat lokalt
                String prefix = item.isLocal() ? "(Sent) " : "";

                setText(prefix + displayMessage);
                setGraphic(null);

                // Mycket enkel stil utan bubblor/färger. Använd standard utseende.
                setStyle(null);
            }
        }
    }


    /**
     * Handles the action of the send button.
     * If a file is attached, it calls the model to send the file; otherwise, it calls the model to send the text message.
     */
    @FXML
    protected void sendMessage() {
        if (model.fileToSendProperty().get() != null) {
            // Om en fil är bifogad, skicka filen och rensa bilagan i modellen
            model.sendFile();
        } else {
            // Annars, skicka textmeddelandet
            model.sendMessage();
        }

        if (messageInput!=null){
            messageInput.requestFocus();
        }
    }

    /**
     * Handles the action of the attach file button.
     * Opens a FileChooser dialog and sets the selected file in the model.
     */
    @FXML
    protected void attachFile() {
        // Hämta scenen från en av kontrollerna

        if (attachFile == null || attachFile.getScene() == null || attachFile.getScene().getWindow() == null) {
            System.err.println("Cannot open file chooser: UI not ready.");
            return;
            }
        Stage stage = (Stage) attachFile.getScene().getWindow();




        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file to attach");

        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            model.setFileToSend(selectedFile);
        }
    }
}