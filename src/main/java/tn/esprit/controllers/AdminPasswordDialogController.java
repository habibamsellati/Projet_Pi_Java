package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import tn.esprit.models.User;

public class AdminPasswordDialogController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private User currentUser;
    private boolean authenticated = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @FXML
    public void handleConfirm() {
        try {
            errorLabel.setText("");

            String enteredPassword = passwordField.getText() == null ? "" : passwordField.getText().trim();

            if (enteredPassword.isEmpty()) {
                errorLabel.setText("Veuillez saisir votre mot de passe.");
                return;
            }

            if (currentUser == null) {
                errorLabel.setText("Utilisateur introuvable.");
                return;
            }

            String realPassword = currentUser.getMotDePasse() == null ? "" : currentUser.getMotDePasse().trim();

            if (enteredPassword.equals(realPassword)) {
                authenticated = true;
                dialogStage.close();
            } else {
                errorLabel.setText("Mot de passe incorrect.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur de vérification.");
        }
    }

    @FXML
    public void handleCancel() {
        authenticated = false;
        dialogStage.close();
    }
}