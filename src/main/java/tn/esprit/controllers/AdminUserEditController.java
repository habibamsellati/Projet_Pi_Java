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
import javafx.scene.control.Control;
import java.util.regex.Pattern;

public class AdminUserEditController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÿ\\s'-]{2,30}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\d{8}$");

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label editTitleLabel;

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
    private User userToEdit;

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
    }

    public void setContext(User currentUser, User userToEdit) {
        this.currentUser = currentUser;
        this.userToEdit = userToEdit;

        if (currentUser != null) {
            String fullName = (safe(currentUser.getNom()) + " " + safe(currentUser.getPrenom())).trim();
            if (fullName.isEmpty()) {
                fullName = safe(currentUser.getEmail());
            }

            userNameLabel.setText(fullName);
            userRoleLabel.setText(safe(currentUser.getRole()).toUpperCase());
        }

        if (userToEdit != null) {
            editTitleLabel.setText((safe(userToEdit.getPrenom()) + " " + safe(userToEdit.getNom())).trim());
            prenomField.setText(safe(userToEdit.getPrenom()));
            nomField.setText(safe(userToEdit.getNom()));
            emailField.setText(safe(userToEdit.getEmail()));
            roleComboBox.setValue(safe(userToEdit.getRole()));
            statutComboBox.setValue(safe(userToEdit.getStatut()));
            sexeComboBox.setValue(safe(userToEdit.getSexe()));

            if (userToEdit.getTelephone() != 0) {
                telephoneField.setText(String.valueOf(userToEdit.getTelephone()));
            } else {
                telephoneField.clear();
            }
        }
    }

    @FXML
    public void handleUpdateUser() {
        try {
            resetStyles();
            messageLabel.setText("");

            String prenom = prenomField.getText().trim();
            String nom = nomField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String role = roleComboBox.getValue();
            String statut = statutComboBox.getValue();
            String sexe = sexeComboBox.getValue();
            String telText = telephoneField.getText().trim();
            String newPassword = passwordField.getText().trim();

            if (!NAME_PATTERN.matcher(prenom).matches()) {
                markInvalid(prenomField);
                throw new Exception("Prénom invalide");
            }

            if (!NAME_PATTERN.matcher(nom).matches()) {
                markInvalid(nomField);
                throw new Exception("Nom invalide");
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                markInvalid(emailField);
                throw new Exception("Email invalide");
            }

            User existing = userService.findByEmail(email);
            if (existing != null && existing.getId() != userToEdit.getId()) {
                markInvalid(emailField);
                throw new Exception("Email déjà utilisé");
            }

            int tel = 0;
            if (!telText.isEmpty()) {
                if (!PHONE_PATTERN.matcher(telText).matches()) {
                    markInvalid(telephoneField);
                    throw new Exception("Téléphone invalide (8 chiffres)");
                }
                tel = Integer.parseInt(telText);
            }

            if (!newPassword.isEmpty()) {
                if (newPassword.length() < 6 ||
                        !newPassword.matches(".*[A-Za-z].*") ||
                        !newPassword.matches(".*\\d.*")) {
                    markInvalid(passwordField);
                    throw new Exception("Mot de passe faible");
                }
            }

            userToEdit.setPrenom(prenom);
            userToEdit.setNom(nom);
            userToEdit.setEmail(email);
            userToEdit.setRole(role);
            userToEdit.setStatut(statut);
            userToEdit.setSexe(sexe);
            userToEdit.setTelephone(tel);

            userService.updateUserByAdmin(userToEdit, newPassword);

            goToUsers();

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    private void markInvalid(Control c) {
        c.setStyle("-fx-border-color: red; -fx-border-width: 2;");
    }

    private void resetStyles() {
        prenomField.setStyle("");
        nomField.setStyle("");
        emailField.setStyle("");
        telephoneField.setStyle("");
        passwordField.setStyle("");
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
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture page utilisateurs : " + e.getMessage());
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

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
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