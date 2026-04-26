package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainCommandeAPP extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterCommande.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("Artefact - Gestion des Commandes");
            primaryStage.setScene(new Scene(root, 1100, 760));
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Erreur fatale Commande : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

