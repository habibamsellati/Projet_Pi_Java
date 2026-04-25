package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField txtNom, txtPrenom, txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbSexe, cbRole;
    @FXML private Label lblError;
    @FXML private Button btnRegister;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void handleRegister() {
        String nom = txtNom.getText().trim();
        String prenom = txtPrenom.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();
        String sexe = cbSexe.getValue();
        String role = cbRole.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || sexe == null || role == null) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User u = new User();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setSexe(sexe);
        u.setRole(role);
        u.setStatut(User.STATUT_ACTIF);

        try {
            serviceUser.inscrire(u, password);
            handleGoToLogin();
        } catch (Exception e) {
            showError("Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToLogin() throws IOException {
        Stage stage = (Stage) btnRegister.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        stage.setScene(new Scene(root, 1000, 700));
        stage.centerOnScreen();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}
