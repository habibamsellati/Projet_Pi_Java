package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import tn.esprit.services.UserService;
import tn.esprit.utils.ForgotPasswordSession;

public class ResetPasswordController {

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    public void handleResetPassword() {
        try {
            newPasswordField.setStyle("");
            confirmPasswordField.setStyle("");
            messageLabel.setText("");

            String newPassword = newPasswordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (!ForgotPasswordSession.hasPendingRequest()) {
                messageLabel.setText("Aucune réinitialisation en attente.");
                return;
            }

            if (ForgotPasswordSession.isExpired()) {
                messageLabel.setText("La demande a expiré. Veuillez recommencer.");
                return;
            }

            if (newPassword.isEmpty()) {
                newPasswordField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("Le mot de passe est obligatoire.");
                return;
            }

            if (newPassword.length() < 6) {
                newPasswordField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("Le mot de passe doit contenir au moins 6 caractères.");
                return;
            }

            if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
                newPasswordField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("Le mot de passe doit contenir au moins une lettre et un chiffre.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                confirmPasswordField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("Les mots de passe ne correspondent pas.");
                return;
            }

            userService.updatePasswordByEmail(ForgotPasswordSession.getEmail(), newPassword);
            ForgotPasswordSession.clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) newPasswordField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur mise à jour mot de passe.");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) newPasswordField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur ouverture connexion.");
        }
    }
}