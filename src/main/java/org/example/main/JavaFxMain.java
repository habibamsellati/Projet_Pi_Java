package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JavaFxMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Chargement du fichier FXML
        // Assure-toi que le nom correspond exactement au fichier dans src/main/resources
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/EspaceLivreur.fxml"));
        Parent parent = fxmlLoader.load();

        // 2. Création de la scène
        Scene scene = new Scene(parent);

        // 3. Configuration de la fenêtre (Stage)
        stage.setScene(scene);
        stage.setTitle("Gestion des Livraisons - AfkArt");

        // Optionnel : Désactiver le redimensionnement pour garder ton design propre
        stage.setResizable(true);

        stage.show();
    }

    public static void main(String[] args) {
        // --- CORRECTIF CRUCIAL POUR LA MAP (WebView) ---
        // Force le rendu logiciel pour éviter les tuiles mal alignées sur Windows
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.vsync", "false");

        // Désactive l'accélération matérielle spécifique à WebKit pour plus de stabilité
        System.setProperty("sun.java2d.opengl", "false");

        // Lancement de l'application
        launch(args);
    }
}