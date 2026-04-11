package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.models.User;

public class ClientDashboardController {

    @FXML
    private Label userNameLabel;

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