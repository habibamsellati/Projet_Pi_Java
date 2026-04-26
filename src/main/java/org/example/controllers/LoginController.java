package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.AuthService;
import org.example.utils.SceneNavigator;

public class LoginController {
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfMotDePasse;
    @FXML private Label lblInfo;

    private final AuthService authService = new AuthService();

    @FXML
    void handleConnexion(ActionEvent event) {
        try {
            User user = authService.login(tfEmail.getText(), pfMotDePasse.getText());
            org.example.utils.SessionManager.login(user);
            SessionManager.utilisateurConnecte = new SessionManager.Utilisateur(
                    user.getId(),
                    user.getNom(),
                    user.getPrenom(),
                    user.getEmail(),
                    user.getRole() == null ? "" : user.getRole().name()
            );

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            if (user.getRole() == Role.CLIENT) {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
                stage.setScene(new Scene(root, 1200, 750));
                stage.setTitle("Artefact");
                stage.centerOnScreen();
                stage.show();
            } else {
                SceneNavigator.openDashboardFor(user, stage);
            }
        } catch (Exception e) {
            lblInfo.setText(e.getMessage());
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleEffacer() {
        tfEmail.clear();
        pfMotDePasse.clear();
        lblInfo.setText("");
    }
}

