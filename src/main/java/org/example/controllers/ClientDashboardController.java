package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.example.models.Role;
import org.example.models.User;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

public class ClientDashboardController {
    @FXML private Label lblBienvenue;

    @FXML
    void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getRole() != Role.CLIENT) {
            lblBienvenue.setText("Accès refusé");
            return;
        }
        lblBienvenue.setText("Bienvenue " + user.getNomComplet() + " (CLIENT)");
    }

    @FXML
    void handleVoirArticles(ActionEvent event) {
        navigate(event, "/fxml/BlogView.fxml", "Notre Blog", 1320, 860);
    }

    @FXML
    void handleNouvelleCommande(ActionEvent event) {
        navigate(event, "/fxml/PanierClient.fxml", "Mon Panier", 1000, 760);
    }

    @FXML
    void handleMesCommandes(ActionEvent event) {
        navigate(event, "/fxml/HistoriqueCommandes.fxml", "Historique des commandes", 1100, 760);
    }

    @FXML
    void handleVoirCatalogue(ActionEvent event) {
        navigate(event, "/fxml/ClientCatalogue.fxml", "Catalogue", 1320, 860);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            SessionManager.logout();
            SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void navigate(ActionEvent event, String fxml, String title, double width, double height) {
        try {
            SceneNavigator.navigate(event, fxml, title, width, height);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}

