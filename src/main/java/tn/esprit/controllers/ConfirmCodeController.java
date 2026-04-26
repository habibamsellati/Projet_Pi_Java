package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.services.UserService;
import tn.esprit.utils.EmailConfirmationSession;

public class ConfirmCodeController {

    @FXML
    private TextField codeField;

    @FXML
    private Label messageLabel;

    @FXML
    private Label emailInfoLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        String email = EmailConfirmationSession.getEmail();
        if (email != null && !email.isEmpty()) {
            emailInfoLabel.setText("Un code a été envoyé à " + email + ".");
        } else {
            emailInfoLabel.setText("Un code a été envoyé à votre email.");
        }
    }

    @FXML
    public void handleConfirm() {
        try {
            String input = codeField.getText().trim();

            if (input.isEmpty()) {
                messageLabel.setText("Veuillez saisir le code.");
                return;
            }

            if (EmailConfirmationSession.isExpired()) {
                messageLabel.setText("Code expiré. Veuillez vous reconnecter pour recevoir un nouveau code.");
                return;
            }

            if (!EmailConfirmationSession.verify(input)) {
                messageLabel.setText("Code incorrect.");
                return;
            }

            userService.activateUserByEmail(EmailConfirmationSession.getEmail());
            EmailConfirmationSession.clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur lors de la confirmation.");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur ouverture connexion.");
        }
    }
}