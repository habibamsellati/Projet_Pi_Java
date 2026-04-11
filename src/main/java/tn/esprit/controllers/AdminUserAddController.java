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
            messageLabel.setText("");

            String prenom = prenomField.getText().trim();
            String nom = nomField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            String role = roleComboBox.getValue();
            String statut = statutComboBox.getValue();
            String sexe = sexeComboBox.getValue();
            String telephoneText = telephoneField.getText().trim();

            if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
                throw new Exception("Veuillez remplir tous les champs obligatoires.");
            }

            if (!email.contains("@")) {
                throw new Exception("Email invalide.");
            }

            if (userService.emailExists(email)) {
                throw new Exception("Cet email existe déjà.");
            }

            int telephone = 0;
            if (!telephoneText.isEmpty()) {
                try {
                    telephone = Integer.parseInt(telephoneText);
                } catch (NumberFormatException e) {
                    throw new Exception("Le téléphone doit être un nombre.");
                }
            }

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

            goToUsers();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
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