package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lance l’écran CRUD brut e-commerce V1 (articles, commandes panier, commentaires).
 */
public class MainEcommerceV1 extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/fxml/ModuleEcommerceV1.fxml");
            if (fxmlUrl == null) {
                System.err.println("ERREUR : ModuleEcommerceV1.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            primaryStage.setTitle("Artefact — E-commerce V1 (CRUD)");
            primaryStage.setScene(new Scene(root, 980, 720));
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
