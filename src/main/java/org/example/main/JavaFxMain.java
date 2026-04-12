package org.example.main; // Assure-toi que le dossier est bien src/main/java/org/example/main

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JavaFxMain extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // 1. Chargement de TON fichier FXML (le nom doit être identique à celui dans resources)
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
        Parent parent = fxmlLoader.load();

        // 2. On crée la scène (pas besoin de dimensions, le FXML définit déjà 800x650)
        Scene scene = new Scene(parent);

        // 3. Configuration du stage
        stage.setScene(scene);
        stage.setTitle("Gestion des Livraisons - Ajouter");

        // Empêche de déformer ton beau design
        stage.setResizable(false);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}