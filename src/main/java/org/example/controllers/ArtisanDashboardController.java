package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.example.models.Role;
import org.example.models.User;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

public class ArtisanDashboardController {
    @FXML private Label lblBienvenue;

    @FXML
    void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null || (user.getRole() != Role.ARTISANT && user.getRole() != Role.ADMIN)) {
            lblBienvenue.setText("Accès refusé");
            return;
        }
        String roleLabel = user.getRole() == Role.ADMIN ? "ADMIN" : "ARTISANT";
        lblBienvenue.setText("Bienvenue " + user.getNomComplet() + " (" + roleLabel + ")");
    }

    @FXML
    void handleNouvelArticle(ActionEvent event) {
        navigate(event, "/fxml/AjouterArticle.fxml", "Créer un article");
    }

    @FXML
    void handleVoirArticles(ActionEvent event) {
        navigate(event, "/fxml/AfficherArticles.fxml", "Liste des Articles");
    }

    @FXML
    void handleVoirProduits(ActionEvent event) {
        navigate(event, "/fxml/ProduitsView.fxml", "Produits recyclables");
    }

    @FXML
    void handleVoirPropositions(ActionEvent event) {
        navigate(event, "/fxml/PropositionsView.fxml", "Propositions");
    }

    @FXML
    void handleNouvelleReclamation(ActionEvent event) {
        navigate(event, "/fxml/AjouterReclamation.fxml", "Nouvelle Réclamation");
    }

    @FXML
    void handleMesReclamations(ActionEvent event) {
        navigate(event, "/fxml/ReclamationsClient.fxml", "Mes Réclamations");
    }

    @FXML
    void handleGererCommandes(ActionEvent event) {
        navigate(event, "/fxml/AfficherCommandes.fxml", "Gestion des Commandes");
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

    private void navigate(ActionEvent event, String fxml, String title) {
        try {
            SceneNavigator.navigate(event, fxml, title, 1000, 700);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}

