package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.utils.ForgotPasswordSession;

import java.util.UUID;
import java.util.regex.Pattern;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void handleSendCode() {
        try {
            messageLabel.setText("");
            emailField.setStyle("");

            String email = emailField.getText().trim().toLowerCase();

            if (email.isEmpty()) {
                emailField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("L'email est obligatoire.");
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                emailField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("Format d'email invalide.");
                return;
            }

            if (!userService.userExistsByEmail(email)) {
                emailField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                messageLabel.setText("Aucun compte trouvé avec cet email.");
                return;
            }

            String token = UUID.randomUUID().toString().replace("-", "");

            ForgotPasswordSession.start(email, token);
            emailService.sendForgotPasswordEmail(email, token);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/email-sent.fxml"));
            Parent root = loader.load();

            EmailSentController controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur envoi email : " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur ouverture connexion.");
        }
    }
}