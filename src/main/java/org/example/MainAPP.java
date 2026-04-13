package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainAPP extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Chargement avec le chemin correspondant à ta structure resources/fxml/
            java.net.URL fxmlUrl = getClass().getResource("/fxml/ModuleEvenementReservation.fxml");

            if (fxmlUrl == null) {
                System.err.println("ERREUR : Fichier FXML introuvable dans resources/fxml/");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 700);

            primaryStage.setTitle("Artefact - Événements & Réservations");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Erreur fatale : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
