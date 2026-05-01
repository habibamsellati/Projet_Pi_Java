package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.example.services.AIService;
import org.example.utils.SessionStore;

public class ChatbotController {

    @FXML
    private VBox chatHistory;
    @FXML
    private TextField txtMessage;
    @FXML
    private ScrollPane scrollPane;

    private final AIService aiService = new AIService();

    @FXML
    public void initialize() {
        // Message de bienvenue automatique
        addMessage("IA Concierge",
                "Bonjour ! Je suis votre assistant IA AfkArt. Comment puis-je vous aider aujourd'hui ?", false);
    }

    @FXML
    private void handleSendMessage() {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty())
            return;

        addMessage("Vous", msg, true);
        txtMessage.clear();

        // Simulation de réflexion de l'IA
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
            }
            String response = aiService.getChatResponse(msg, SessionStore.isArtisan());
            Platform.runLater(() -> addMessage("IA Concierge", response, false));
        }).start();
    }

    private void addMessage(String sender, String text, boolean isUser) {
        VBox container = new VBox(5);
        container.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label lblSender = new Label(sender);
        lblSender.setStyle("-fx-font-size: 10px; -fx-text-fill: #9e8e82; -fx-font-weight: bold;");

        TextFlow bubble = new TextFlow(new Text(text));
        bubble.setMaxWidth(280);
        bubble.setPadding(new javafx.geometry.Insets(12));

        if (isUser) {
            bubble.setStyle("-fx-background-color: #c4704b; -fx-background-radius: 15 15 0 15;");
            ((Text) bubble.getChildren().get(0)).setFill(Color.WHITE);
        } else {
            bubble.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15 15 15 0;");
            ((Text) bubble.getChildren().get(0)).setFill(Color.web("#3d3229"));
        }

        container.getChildren().addAll(lblSender, bubble);
        chatHistory.getChildren().add(container);

        // Auto-scroll
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleClose() {
        // Dans une integration réelle, on fermerait le popup ou le side panel
        chatHistory.getScene().getWindow().hide();
    }
}
