package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.services.UserService;

import java.sql.Timestamp;
import java.util.regex.Pattern;

public class AdminUserAddController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField nomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private ComboBox<String> statutComboBox;

    @FXML
    private ComboBox<String> sexeComboBox;

    @FXML
    private TextField telephoneField;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private User currentUser;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÿ\\s'-]{2,30}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\d{8}$");

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(
                "ADMIN", "CLIENT", "ARTISAN", "LIVREUR"
        ));

        statutComboBox.setItems(FXCollections.observableArrayList(
                "actif", "inactif"
        ));

        sexeComboBox.setItems(FXCollections.observableArrayList(
                "Homme", "Femme"
        ));

        roleComboBox.setValue("CLIENT");
        statutComboBox.setValue("actif");
        sexeComboBox.setValue("Homme");
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;

        if (currentUser != null) {
            String fullName = ((safe(currentUser.getNom()) + " " + safe(currentUser.getPrenom())).trim());
            if (fullName.isEmpty()) {
                fullName = safe(currentUser.getEmail());
            }

            userNameLabel.setText(fullName);
            userRoleLabel.setText(safe(currentUser.getRole()).toUpperCase());
        }
    }

    @FXML
    public void handleAddUser() {
        try {
            resetValidationStyles();
            messageLabel.setText("");

            String prenom = prenomField.getText().trim();
            String nom = nomField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String password = passwordField.getText().trim();
            String role = roleComboBox.getValue();
            String statut = statutComboBox.getValue();
            String sexe = sexeComboBox.getValue();
            String telephoneText = telephoneField.getText().trim();

            validatePrenom(prenom);
            validateNom(nom);
            validateEmail(email);
            validatePassword(password);
            validateRole(role);
            validateStatut(statut);
            validateSexe(sexe);
            validateTelephone(telephoneText);

            if (userService.emailExists(email)) {
                markInvalid(emailField);
                throw new Exception("Cet email existe déjà.");
            }

            int telephone = Integer.parseInt(telephoneText);

            User user = new User();
            user.setPrenom(prenom);
            user.setNom(nom);
            user.setEmail(email);
            user.setMotDePasse(password);
            user.setRole(role);
            user.setStatut(statut);
            user.setSexe(sexe);
            user.setTelephone(telephone);
            user.setDateCreation(new Timestamp(System.currentTimeMillis()));

            userService.addUser(user);

            messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            messageLabel.setText("Utilisateur ajouté avec succès.");

            goToUsers();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            messageLabel.setText(e.getMessage());
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

    private void validateRole(String role) throws Exception {
        if (role == null || role.trim().isEmpty()) {
            markInvalid(roleComboBox);
            throw new Exception("Veuillez sélectionner un rôle.");
        }
    }

    private void validateStatut(String statut) throws Exception {
        if (statut == null || statut.trim().isEmpty()) {
            markInvalid(statutComboBox);
            throw new Exception("Veuillez sélectionner un statut.");
        }
    }

    private void validateSexe(String sexe) throws Exception {
        if (sexe == null || sexe.trim().isEmpty()) {
            markInvalid(sexeComboBox);
            throw new Exception("Veuillez sélectionner le sexe.");
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

    private void markInvalid(Control control) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 8;");
    }

    private void resetValidationStyles() {
        prenomField.setStyle("");
        nomField.setStyle("");
        emailField.setStyle("");
        passwordField.setStyle("");
        telephoneField.setStyle("");
        roleComboBox.setStyle("");
        statutComboBox.setStyle("");
        sexeComboBox.setStyle("");
    }

    @FXML
    public void goToUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-users.fxml"));
            Parent root = loader.load();

            AdminUsersController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur retour utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    public void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-dashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture dashboard : " + e.getMessage());
        }
    }

    @FXML
    public void goToFront() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/front-home.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture front : " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        goToUsers();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
}