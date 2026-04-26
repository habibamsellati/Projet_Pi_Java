package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.services.UserService;
import javafx.scene.control.Control;
import java.util.regex.Pattern;

import java.text.SimpleDateFormat;

public class ProfileController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÿ\\s'-]{2,30}$");

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileEmailLabel;

    @FXML
    private Label roleValueLabel;

    @FXML
    private Label sexeValueLabel;

    @FXML
    private Label statutValueLabel;

    @FXML
    private Label dateCreationValueLabel;

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        loadUserData();
    }

    private void loadUserData() {
        if (currentUser == null) {
            return;
        }

        String fullName = (safe(currentUser.getPrenom()) + " " + safe(currentUser.getNom())).trim();
        if (fullName.isEmpty()) {
            fullName = safe(currentUser.getEmail());
        }

        userNameLabel.setText(fullName);
        userRoleLabel.setText("(" + safe(currentUser.getRole()).toUpperCase() + ")");

        profileNameLabel.setText(fullName);
        profileEmailLabel.setText(safe(currentUser.getEmail()));

        roleValueLabel.setText(safe(currentUser.getRole()).isEmpty() ? "-" : safe(currentUser.getRole()).toUpperCase());
        sexeValueLabel.setText(safe(currentUser.getSexe()).isEmpty() ? "-" : safe(currentUser.getSexe()).toUpperCase());
        statutValueLabel.setText(safe(currentUser.getStatut()).isEmpty() ? "-" : safe(currentUser.getStatut()));

        if (currentUser.getDateCreation() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            dateCreationValueLabel.setText(sdf.format(currentUser.getDateCreation()));
        } else {
            dateCreationValueLabel.setText("-");
        }

        nomField.setText(safe(currentUser.getNom()));
        prenomField.setText(safe(currentUser.getPrenom()));
        emailField.setText(safe(currentUser.getEmail()));
    }

    @FXML
    public void handleUpdateProfile() {
        try {
            resetStyles();
            messageLabel.setText("");

            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();

            if (nom.isEmpty()) {
                markInvalid(nomField);
                throw new Exception("Nom obligatoire.");
            }

            if (!NAME_PATTERN.matcher(nom).matches()) {
                markInvalid(nomField);
                throw new Exception("Nom invalide.");
            }

            if (prenom.isEmpty()) {
                markInvalid(prenomField);
                throw new Exception("Prénom obligatoire.");
            }

            if (!NAME_PATTERN.matcher(prenom).matches()) {
                markInvalid(prenomField);
                throw new Exception("Prénom invalide.");
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                markInvalid(emailField);
                throw new Exception("Email invalide.");
            }

            User existing = userService.findByEmail(email);
            if (existing != null && existing.getId() != currentUser.getId()) {
                markInvalid(emailField);
                throw new Exception("Email déjà utilisé.");
            }

            currentUser.setNom(nom);
            currentUser.setPrenom(prenom);
            currentUser.setEmail(email);

            userService.updateProfile(currentUser);

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Profil mis à jour ✔");

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void handleUpdatePassword() {
        try {
            resetStyles();
            messageLabel.setText("");

            String newPassword = newPasswordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (newPassword.isEmpty()) {
                markInvalid(newPasswordField);
                throw new Exception("Mot de passe requis.");
            }

            if (newPassword.length() < 6) {
                markInvalid(newPasswordField);
                throw new Exception("Minimum 6 caractères.");
            }

            if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
                markInvalid(newPasswordField);
                throw new Exception("Mot de passe doit contenir lettre + chiffre.");
            }

            if (!newPassword.equals(confirmPassword)) {
                markInvalid(confirmPasswordField);
                throw new Exception("Les mots de passe ne correspondent pas.");
            }

            userService.updatePassword(currentUser.getId(), newPassword);

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Mot de passe mis à jour ✔");

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }
    private void markInvalid(Control c) {
        c.setStyle("-fx-border-color: red; -fx-border-width: 2;");
    }

    private void resetStyles() {
        nomField.setStyle("");
        prenomField.setStyle("");
        emailField.setStyle("");
        newPasswordField.setStyle("");
        confirmPasswordField.setStyle("");
    }

    @FXML
    public void handleOpenProfile() {
        // déjà sur la page profil
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            String role = safe(currentUser.getRole()).toUpperCase();

            FXMLLoader loader;
            Parent root;

            switch (role) {
                case "ADMIN":
                    loader = new FXMLLoader(getClass().getResource("/view/admin-front.fxml"));
                    root = loader.load();

                    AdminFrontController adminController = loader.getController();
                    adminController.setUser(currentUser);
                    break;

                case "CLIENT":
                    loader = new FXMLLoader(getClass().getResource("/view/client_dashboard.fxml"));
                    root = loader.load();

                    ClientDashboardController clientController = loader.getController();
                    clientController.setUser(currentUser);
                    break;

                case "ARTISAN":
                    loader = new FXMLLoader(getClass().getResource("/view/role-space.fxml"));
                    root = loader.load();

                    RoleSpaceController artisanController = loader.getController();
                    artisanController.configure(
                            currentUser,
                            "Espace Artisan",
                            "Gérez vos oeuvres, commandes et evenements.",
                            "Catalogue",
                            "Gestion des oeuvres.",
                            "Commandes",
                            "Suivi des ventes.",
                            "Evenements",
                            "Planning et reservation."
                    );
                    break;

                case "LIVREUR":
                    loader = new FXMLLoader(getClass().getResource("/view/role-space.fxml"));
                    root = loader.load();

                    RoleSpaceController livreurController = loader.getController();
                    livreurController.configure(
                            currentUser,
                            "Espace Livreur",
                            "Gérez vos missions et livraisons.",
                            "Planning",
                            "Consulter les missions.",
                            "Livraisons",
                            "Suivi des livraisons.",
                            "Historique",
                            "Historique des missions."
                    );
                    break;

                default:
                    throw new RuntimeException("Rôle non reconnu : " + role);
            }

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Erreur retour accueil : " + e.getMessage());
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