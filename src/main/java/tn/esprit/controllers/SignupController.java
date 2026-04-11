package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.services.UserService;

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
                throw new Exception("Veuillez remplir tous les champs obligatoires.");
            }

            if (!email.contains("@")) {
                throw new Exception("Email invalide.");
            }

            if (password.length() < 6) {
                throw new Exception("Le mot de passe doit contenir au moins 6 caractères.");
            }

            int telephone;
            try {
                telephone = Integer.parseInt(telephoneText);
            } catch (NumberFormatException e) {
                throw new Exception("Le numéro de téléphone doit être un nombre.");
            }

            if (userService.emailExists(email)) {
                throw new Exception("Cet email existe déjà.");
            }

            User user = new User();
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setEmail(email);
            user.setMotDePasse(password);
            user.setTelephone(telephone);
            user.setRole(role);
            user.setSexe(sexe);

            userService.addUser(user);

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Inscription réussie.");

            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
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

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        telephoneField.clear();
        sexeComboBox.setValue("Homme");
        roleComboBox.setValue("CLIENT");
    }
}