package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.services.UserService;

import java.util.regex.Pattern;

public class SignupController {

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField telephoneField;

    @FXML
    private ComboBox<String> sexeComboBox;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÿ\\s'-]{2,30}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\d{8}$");

    @FXML
    public void initialize() {
        sexeComboBox.setItems(FXCollections.observableArrayList(
                "Homme",
                "Femme"
        ));

        roleComboBox.setItems(FXCollections.observableArrayList(
                "CLIENT",
                "ADMIN",
                "ARTISAN",
                "LIVREUR"
        ));

        sexeComboBox.setValue("Homme");
        roleComboBox.setValue("CLIENT");
    }

    @FXML
    public void handleSignup() {
        try {
            messageLabel.setText("");

            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            String telephoneText = telephoneField.getText().trim();
            String sexe = sexeComboBox.getValue();
            String role = roleComboBox.getValue();

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || telephoneText.isEmpty()) {
                throw new Exception("Veuillez remplir tous les champs.");
            }

            int telephone = Integer.parseInt(telephoneText);

            if (userService.emailExists(email)) {
                throw new Exception("Email déjà utilisé.");
            }

            // 🔥 utilisateur inactif
            User user = new User();
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setEmail(email);
            user.setMotDePasse(password);
            user.setTelephone(telephone);
            user.setRole(role);
            user.setSexe(sexe);
            user.setStatut("inactif");

            userService.addUser(user);

            // 🔥 générer code
            String code = String.valueOf((int)(Math.random() * 900000) + 100000);

            // 🔥 stocker en session
            tn.esprit.utils.EmailConfirmationSession.start(email, code);

            // 🔥 envoyer email
            new tn.esprit.services.EmailService().sendConfirmationEmail(email, code);

            // 🔥 ouvrir page confirmation
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/confirm-code.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Erreur ouverture login");
        }
    }

    private void validateNom(String nom) throws Exception {
        if (nom.isEmpty()) {
            markInvalid(nomField);
            throw new Exception("Le nom est obligatoire.");
        }
        if (!NAME_PATTERN.matcher(nom).matches()) {
            markInvalid(nomField);
            throw new Exception("Le nom doit contenir uniquement des lettres et avoir entre 2 et 30 caractères.");
        }
    }

    private void validatePrenom(String prenom) throws Exception {
        if (prenom.isEmpty()) {
            markInvalid(prenomField);
            throw new Exception("Le prénom est obligatoire.");
        }
        if (!NAME_PATTERN.matcher(prenom).matches()) {
            markInvalid(prenomField);
            throw new Exception("Le prénom doit contenir uniquement des lettres et avoir entre 2 et 30 caractères.");
        }
    }

    private void validateEmail(String email) throws Exception {
        if (email.isEmpty()) {
            markInvalid(emailField);
            throw new Exception("L'email est obligatoire.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            markInvalid(emailField);
            throw new Exception("Format d'email invalide. Exemple : nom@gmail.com");
        }
    }

    private void validatePassword(String password) throws Exception {
        if (password.isEmpty()) {
            markInvalid(passwordField);
            throw new Exception("Le mot de passe est obligatoire.");
        }
        if (password.length() < 6) {
            markInvalid(passwordField);
            throw new Exception("Le mot de passe doit contenir au moins 6 caractères.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            markInvalid(passwordField);
            throw new Exception("Le mot de passe doit contenir au moins une lettre et un chiffre.");
        }
    }

    private void validateTelephone(String telephoneText) throws Exception {
        if (telephoneText.isEmpty()) {
            markInvalid(telephoneField);
            throw new Exception("Le téléphone est obligatoire.");
        }
        if (!PHONE_PATTERN.matcher(telephoneText).matches()) {
            markInvalid(telephoneField);
            throw new Exception("Le numéro de téléphone doit contenir exactement 8 chiffres.");
        }
    }

    private void validateSexe(String sexe) throws Exception {
        if (sexe == null || sexe.trim().isEmpty()) {
            markInvalid(sexeComboBox);
            throw new Exception("Veuillez sélectionner le sexe.");
        }
    }

    private void validateRole(String role) throws Exception {
        if (role == null || role.trim().isEmpty()) {
            markInvalid(roleComboBox);
            throw new Exception("Veuillez sélectionner le rôle.");
        }
    }

    private void markInvalid(Control control) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 8;");
    }

    private void resetValidationStyles() {
        nomField.setStyle("");
        prenomField.setStyle("");
        emailField.setStyle("");
        passwordField.setStyle("");
        telephoneField.setStyle("");
        sexeComboBox.setStyle("");
        roleComboBox.setStyle("");
    }

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        telephoneField.clear();
        sexeComboBox.setValue("Homme");
        roleComboBox.setValue("CLIENT");
        resetValidationStyles();
    }
}