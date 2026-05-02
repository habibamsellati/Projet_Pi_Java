package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.models.User;
import org.example.services.EmailService;
import org.example.services.UserManagementService;
import org.example.utils.PasswordResetSession;
import org.example.utils.SceneNavigator;

public class ForgotPasswordController {

    @FXML private TextField tfEmail;
    @FXML private Label lblMessage;

    private final UserManagementService userManagementService = new UserManagementService();
    private final EmailService emailService = new EmailService();

    @FXML
    void handleEnvoyerCode(ActionEvent event) {
        try {
            String email = tfEmail.getText() == null ? "" : tfEmail.getText().trim();
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Veuillez saisir votre email.");
            }

            User user = userManagementService.findUserByEmail(email);
            if (user == null) {
                throw new IllegalArgumentException("Aucun compte trouvé avec cet email.");
            }

            String code = PasswordResetSession.start(user.getEmail());
            boolean sent = emailService.envoyerCodeReinitialisation(user.getEmail(), user.getNomComplet(), code);

            if (sent) {
                lblMessage.setText("Code envoyé par email. Vérifiez votre boîte de réception.");
            } else {
                lblMessage.setText("SMTP non actif : le code a été généré en mode preview console. Utilisez le code affiché dans la console.");
            }

            SceneNavigator.navigate(event, "/fxml/ResetPassword.fxml", "Réinitialiser le mot de passe", 620, 520);
        } catch (Exception e) {
            lblMessage.setText(e.getMessage());
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleRetourLogin(ActionEvent event) {
        try {
            SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception e) {
            lblMessage.setText(e.getMessage());
        }
    }
}

