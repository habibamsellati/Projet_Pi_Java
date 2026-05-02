package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.services.ReclamationWarningService;

public class MainAPP extends Application {

    private final ReclamationWarningService warningService = new ReclamationWarningService();

    @Override
    public void start(Stage primaryStage) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/fxml/Login.fxml");

            if (fxmlUrl == null) {
                System.err.println("ERREUR : Fichier FXML introuvable dans resources/fxml/");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, 520, 420);

            primaryStage.setTitle("Artefact - Connexion");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Démarrer la surveillance des réclamations non traitées
            warningService.demarrer();

            // Arrêter proprement à la fermeture
            primaryStage.setOnCloseRequest(e -> warningService.arreter());

        } catch (Exception e) {
            System.err.println("Erreur fatale : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}