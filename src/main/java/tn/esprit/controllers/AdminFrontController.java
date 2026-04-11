package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.models.User;

public class AdminFrontController {

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
    public void handleOpenDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-password-dialog.fxml"));
            Parent root = loader.load();

            AdminPasswordDialogController controller = loader.getController();
            controller.setUser(currentUser);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(userNameLabel.getScene().getWindow());
            dialogStage.setTitle("Confirmation Admin");
            dialogStage.setResizable(false);
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isAuthenticated()) {
                FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/view/admin-dashboard.fxml"));
                Parent dashboardRoot = dashboardLoader.load();

                AdminDashboardController dashboardController = dashboardLoader.getController();
                dashboardController.setUser(currentUser);

                Stage stage = (Stage) userNameLabel.getScene().getWindow();
                stage.setScene(new Scene(dashboardRoot));
                stage.show();
            }

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