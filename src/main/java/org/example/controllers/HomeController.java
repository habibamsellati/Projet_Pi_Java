package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController {

    @FXML
    private Button btnLogin, btnLogout;

    @FXML
    public void initialize() {
        boolean connected = org.example.utils.SessionStore.isConnected();
        if (btnLogin != null) {
            btnLogin.setVisible(!connected);
            btnLogin.setManaged(!connected);
        }
        if (btnLogout != null) {
            btnLogout.setVisible(connected);
            btnLogout.setManaged(connected);
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        org.example.utils.SessionStore.logout();
        initialize(); // Refresh buttons
    }

    @FXML
    void handleGoToCatalog(ActionEvent event) {
        navigate(event, "/fxml/ModuleEvenementReservation.fxml");
    }

    @FXML
    void handleGoToArticles(ActionEvent event) {
        navigate(event, "/fxml/ModuleEcommerceV1.fxml");
    }

    @FXML
    void handleGoToLogin(ActionEvent event) {
        navigate(event, "/fxml/Login.fxml");
    }

    private void navigate(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
