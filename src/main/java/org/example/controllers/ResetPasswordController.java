package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.services.UserManagementService;
import org.example.utils.PasswordResetSession;
import org.example.utils.SceneNavigator;

public class ResetPasswordController {

    @FXML private TextField tfEmail;
    @FXML private TextField tfCode;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private Label lblMessage;

    private final UserManagementService userManagementService = new UserManagementService();

    @FXML
    void handleResetPassword(ActionEvent event) {
        try {
            String email = tfEmail.getText() == null ? "" : tfEmail.getText().trim();
            String code = tfCode.getText() == null ? "" : tfCode.getText().trim();
            String password = pfNewPassword.getText() == null ? "" : pfNewPassword.getText().trim();
            String confirm = pfConfirmPassword.getText() == null ? "" : pfConfirmPassword.getText().trim();

            if (email.isEmpty() || code.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                throw new IllegalArgumentException("Veuillez remplir tous les champs.");
            }
            if (password.length() < 6) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères.");
            }
            if (!password.equals(confirm)) {
                throw new IllegalArgumentException("La confirmation du mot de passe est incorrecte.");
            }
            if (!PasswordResetSession.matches(email, code)) {
                throw new IllegalArgumentException("Code invalide ou expiré.");
            }

            userManagementService.updatePasswordByEmail(email, password);
            PasswordResetSession.clear();
            new Alert(Alert.AlertType.INFORMATION, "Mot de passe mis à jour avec succès.").showAndWait();
            SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
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

