package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.models.Reclamation;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceReclamation;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ReclamationsClientController {
    @FXML private Label lblBienvenue;
    @FXML private TextField tfSearch;
    @FXML private VBox containerReclamations;
    @FXML private Button btnNouvelleReclamation;

    private ServiceReclamation serviceReclamation;
    private ObservableList<Reclamation> reclamations;
    private User currentUser;

    @FXML
    void initialize() {
        serviceReclamation = new ServiceReclamation();
        currentUser = SessionManager.getCurrentUser();

        // Vérifier l'accès
        if (currentUser == null || (currentUser.getRole() != Role.CLIENT && currentUser.getRole() != Role.ARTISANT)) {
            lblBienvenue.setText("Accès refusé");
            btnNouvelleReclamation.setDisable(true);
            return;
        }

        String roleLabel = currentUser.getRole() == Role.ARTISANT ? "ARTISANT" : "CLIENT";
        lblBienvenue.setText("Mes Réclamations - " + currentUser.getNomComplet() + " (" + roleLabel + ")");
        chargerReclamations();

        // Ajouter listener de recherche
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> filtrerReclamations(newVal));
    }

    @FXML
    void handleNouvelleReclamation(ActionEvent event) {
        try {
            SceneNavigator.navigate(event, "/fxml/AjouterReclamation.fxml", 
                    "Nouvelle Réclamation", 800, 700);
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void chargerReclamations() {
        try {
            List<Reclamation> liste = serviceReclamation.afficherReclamationsClient(currentUser.getId());
            reclamations = FXCollections.observableArrayList(liste);
            afficherReclamations(reclamations);
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void afficherReclamations(List<Reclamation> listeReclamations) {
        containerReclamations.getChildren().clear();

        if (listeReclamations.isEmpty()) {
            Label lblVide = new Label("Aucune réclamation pour le moment");
            lblVide.setStyle("-fx-font-size: 14; -fx-text-fill: #999;");
            containerReclamations.getChildren().add(lblVide);
            return;
        }

        for (Reclamation rec : listeReclamations) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReclamationCard.fxml"));
                Node card = loader.load();
                ReclamationCardController controller = loader.getController();
                controller.setReclamation(rec);
                controller.setParentController(this);
                containerReclamations.getChildren().add(card);
            } catch (Exception e) {
                showError("Erreur de chargement", e.getMessage());
            }
        }
    }

    private void filtrerReclamations(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            afficherReclamations(reclamations);
            return;
        }

        String keywordLower = keyword.toLowerCase();
        List<Reclamation> filtered = reclamations.stream()
                .filter(r -> r.getTitre().toLowerCase().contains(keywordLower)
                        || (r.getDescription() != null && r.getDescription().toLowerCase().contains(keywordLower))
                        || r.getStatut().toLowerCase().contains(keywordLower))
                .toList();

        afficherReclamations(filtered);
    }

    public void refreshReclamations() {
        chargerReclamations();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

