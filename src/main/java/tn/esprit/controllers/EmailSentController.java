package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.utils.ForgotPasswordSession;

public class EmailSentController {

    @FXML
    private Label infoLabel;

    public void setEmail(String email) {
        infoLabel.setText("Un email de réinitialisation a été envoyé à " + email + ". Veuillez vérifier votre boite mail puis cliquer sur le bouton ci-dessous.");
    }

    @FXML
    public void goToResetPassword() {
        try {
            if (!ForgotPasswordSession.hasPendingRequest() || ForgotPasswordSession.isExpired()) {
                infoLabel.setText("La demande a expiré. Veuillez recommencer.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reset-password.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) infoLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            infoLabel.setText("Erreur ouverture page réinitialisation.");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) infoLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            infoLabel.setText("Erreur ouverture connexion.");
        }
    }
}