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

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    public void handleLogin() {
        try {
            errorLabel.setText("");

            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Veuillez remplir tous les champs.");
                return;
            }

            User user = userService.login(email, password);

            if (user == null) {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            redirectByRole(user);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(e.getMessage());
        }
    }

    private void redirectByRole(User user) {
        try {
            String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();

            switch (role) {
                case "ADMIN":
                    loadAdminPage(user);
                    break;

                case "CLIENT":
                    loadClientPage(user);
                    break;

                case "ARTISAN":
                    loadRoleSpace(
                            user,
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
                    loadRoleSpace(
                            user,
                            "Espace Livreur",
                            "Gérez vos oeuvres, commandes et evenements.",
                            "Catalogue",
                            "Gestion des oeuvres.",
                            "Commandes",
                            "Suivi des ventes.",
                            "Evenements",
                            "Planning et reservation."
                    );
                    break;

                default:
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText("Rôle non reconnu : " + role);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Erreur de redirection : " + e.getMessage());
        }
    }

    private void loadAdminPage(User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-front.fxml"));
        Parent root = loader.load();

        AdminFrontController controller = loader.getController();
        controller.setUser(user);

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void loadClientPage(User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/client_dashboard.fxml"));
        Parent root = loader.load();

        ClientDashboardController controller = loader.getController();
        controller.setUser(user);

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void loadRoleSpace(User user,
                               String pageTitle,
                               String pageSubtitle,
                               String card1Title,
                               String card1Subtitle,
                               String card2Title,
                               String card2Subtitle,
                               String card3Title,
                               String card3Subtitle) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/role-space.fxml"));
        Parent root = loader.load();

        RoleSpaceController controller = loader.getController();
        controller.configure(
                user,
                pageTitle,
                pageSubtitle,
                card1Title, card1Subtitle,
                card2Title, card2Subtitle,
                card3Title, card3Subtitle
        );

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void goToSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/signup.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Impossible d'ouvrir la page d'inscription");
        }
    }
}