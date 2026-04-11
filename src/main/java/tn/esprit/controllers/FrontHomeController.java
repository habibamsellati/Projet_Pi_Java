package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;

public class FrontHomeController {

    @FXML
    private Button loginButton;

    @FXML
    private ImageView heroImageView;

    @FXML
    public void initialize() {
        System.out.println("initialize() lancé");
        System.out.println("heroImageView = " + heroImageView);

        try {
            URL imageUrl = getClass().getResource("/image/art.jpg");
            System.out.println("imageUrl = " + imageUrl);

            if (imageUrl == null) {
                System.out.println("Image introuvable dans resources");
                return;
            }

            Image image = new Image(imageUrl.toExternalForm(), false);
            System.out.println("image error = " + image.isError());

            if (image.isError()) {
                System.out.println("Erreur chargement image : " + image.getException());
                return;
            }

            heroImageView.setImage(image);
            heroImageView.setVisible(true);
            heroImageView.setManaged(true);

            System.out.println("Image chargée avec succès");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}