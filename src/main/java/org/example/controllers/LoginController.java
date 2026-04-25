package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;
import org.example.utils.SessionStore;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblError;
    @FXML
    private Button btnLogin;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void handleLogin() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        try {
            if (serviceUser.authentifier(email, password)) {
                User user = serviceUser.trouverParEmail(email);
                System.out.println("DEBUG: user = "
                        + (user == null ? "null" : user.getEmail() + " role=[" + user.getRole() + "]"));
                if (user != null) {
                    SessionStore.login(user);
                    try {
                        navigateToDashboard(user);
                    } catch (Exception navEx) {
                        navEx.printStackTrace();
                        showError("Erreur de navigation: " + navEx.getMessage());
                    }
                } else {
                    showError("Erreur lors de la récupération du profil.");
                }
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur : " + e.getMessage());
        }
    }

    private void navigateToDashboard(User user) throws IOException {
        String fxmlFile = "/fxml/ModuleEvenementReservation.fxml";
        String title = "AfkArt - Événements & Réservations";

        String role = user.getRole() != null ? user.getRole().trim() : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

        System.out.println("DEBUG navigateToDashboard: role=[" + role + "] email=[" + email + "]");

        if ("ADMIN".equalsIgnoreCase(role) || email.contains("admin")) {
            fxmlFile = "/fxml/AdminLayout.fxml";
            title = "AfkArt - Administration Centrale";
        } else if ("ARTISANT".equalsIgnoreCase(role) || "ARTISAN".equalsIgnoreCase(role) || email.contains("artisan")) {
            // L'artisan utilise l'interface front-end avec des privilèges supplémentaires
            fxmlFile = "/fxml/ModuleEvenementReservation.fxml";
            title = "AfkArt - Événements & Réservations (Mode Artisan)";
        }

        System.out.println("DEBUG: Chargement de " + fxmlFile);
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Scene scene = new Scene(root, 1100, 800);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    @FXML
    private void handleGoToRegister() throws IOException {
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Register.fxml"));
        stage.setScene(new Scene(root, 1000, 700));
        stage.centerOnScreen();
    }
}
