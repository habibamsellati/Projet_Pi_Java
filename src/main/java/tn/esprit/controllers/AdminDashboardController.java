package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.models.User;

import java.net.URL;

public class AdminDashboardController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;

        if (user != null) {
            String fullName = (safe(user.getNom()) + " " + safe(user.getPrenom())).trim();
            if (fullName.isEmpty()) {
                fullName = safe(user.getEmail());
            }

            userNameLabel.setText(fullName);
            userRoleLabel.setText(safe(user.getRole()).toUpperCase());
        }
    }

    @FXML
    public void goToDashboard() {
        // Déjà sur dashboard
    }

    @FXML
    public void goToUsers() {
        try {
            URL url = getClass().getResource("/view/admin-users.fxml");
            if (url == null) {
                showError("Fichier introuvable : /view/admin-users.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminUsersController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture page utilisateurs : " + e.getMessage());
        }
    }


    @FXML
    public void goToFront() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/front-home.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture front : " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        goToFront();
    }

    @FXML
    public void handleLogout() {
        try {
            URL url = getClass().getResource("/view/login.fxml");
            if (url == null) {
                showError("Fichier introuvable : /view/login.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur déconnexion : " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenProfile() {
        try {
            URL url = getClass().getResource("/view/profile.fxml");
            if (url == null) {
                showError("Fichier introuvable : /view/profile.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture profil : " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
}