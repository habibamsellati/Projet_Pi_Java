package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import tn.esprit.models.User;
import javafx.scene.image.ImageView;
import java.net.URL;

public class ClientDashboardController {

    @FXML
    private Label userNameLabel;


    @FXML
    private ImageView heroImageView;

    @FXML
    private Label userRoleLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;

        if (user != null) {
            String fullName = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
            if (fullName.isEmpty()) {
                fullName = safe(user.getEmail());
            }

            userNameLabel.setText(fullName);
            userRoleLabel.setText("(" + safe(user.getRole()).toUpperCase() + ")");
        }
    }
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
    public void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/profile.fxml"));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}