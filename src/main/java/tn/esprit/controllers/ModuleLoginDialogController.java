package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.utils.ModuleAccessManager;

public class ModuleLoginDialogController {

    @FXML
    private Label moduleTitleLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private Stage dialogStage;
    private boolean authenticated = false;
    private String moduleKey;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
        moduleTitleLabel.setText("Module : " + moduleKey);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @FXML
    public void handleValidate() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Veuillez remplir le nom d'utilisateur et le mot de passe.");
            return;
        }

        if (ModuleAccessManager.validateAccess(moduleKey, username, password)) {
            authenticated = true;
            dialogStage.close();
        } else {
            messageLabel.setText("Identifiants incorrects pour ce module.");
        }
    }

    @FXML
    public void handleCancel() {
        authenticated = false;
        dialogStage.close();
    }
}