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
            // Initialisation automatique des comptes Admin/Artisan/Client pour la démo
            org.example.utils.SystemInitializer.initializeData();

            // Lancement par défaut sur la Landing Page (Home), comme sur le Web
            java.net.URL fxmlUrl = getClass().getResource("/fxml/Home.fxml");

            if (fxmlUrl == null) {
                System.err.println("ERREUR : Fichier Login.fxml introuvable dans resources/fxml/");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1150, 750);

            primaryStage.setTitle("AfkArt - Accueil Artisanal");
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
