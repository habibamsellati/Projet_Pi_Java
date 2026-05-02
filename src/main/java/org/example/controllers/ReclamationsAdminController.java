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
import org.example.models.ReponseReclamation;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceReclamation;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ReclamationsAdminController {
    @FXML private Label lblBienvenue;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatut;
    @FXML private VBox containerReclamations;

    private ServiceReclamation serviceReclamation;
    private ObservableList<Reclamation> reclamations;
    private User currentUser;

     @FXML
     void initialize() {
         serviceReclamation = new ServiceReclamation();
         currentUser = SessionManager.getCurrentUser();

         // Vérifier l'accès - ADMIN et ARTISANT peuvent accéder
         if (currentUser == null || (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.ARTISANT)) {
             lblBienvenue.setText("Accès refusé");
             return;
         }

        lblBienvenue.setText("Gestion des Réclamations - " + currentUser.getNomComplet());

        // Initialiser le combo box des statuts
        cbStatut.setItems(FXCollections.observableArrayList(
                "Tous les statuts",
                "en_attente",
                "en_cours",
                "resolu",
                "rejete"
        ));
        cbStatut.setValue("Tous les statuts");

        chargerReclamations();

        // Ajouter listeners de filtrage
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> filtrer());
        cbStatut.valueProperty().addListener((obs, oldVal, newVal) -> filtrer());
    }

    private void chargerReclamations() {
        try {
            List<Reclamation> liste = serviceReclamation.afficherToutesReclamations();
            reclamations = FXCollections.observableArrayList(liste);
            afficherReclamations(reclamations);
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void afficherReclamations(List<Reclamation> listeReclamations) {
        containerReclamations.getChildren().clear();

        if (listeReclamations.isEmpty()) {
            Label lblVide = new Label("Aucune réclamation");
            lblVide.setStyle("-fx-font-size: 14; -fx-text-fill: #999;");
            containerReclamations.getChildren().add(lblVide);
            return;
        }

        for (Reclamation rec : listeReclamations) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReclamationAdminCard.fxml"));
                Node card = loader.load();
                ReclamationAdminCardController controller = loader.getController();
                controller.setReclamation(rec);
                controller.setParentController(this);
                containerReclamations.getChildren().add(card);
            } catch (Exception e) {
                showError("Erreur de chargement", e.getMessage());
            }
        }
    }

    private void filtrer() {
        if (reclamations == null) return;

        String keyword = tfSearch.getText().toLowerCase();
        String statut = cbStatut.getValue();

        List<Reclamation> filtered = reclamations.stream()
                .filter(r -> {
                    boolean matchStatut = statut.equals("Tous les statuts") || r.getStatut().equals(statut);
                    boolean matchKeyword = keyword.isEmpty() 
                            || r.getTitre().toLowerCase().contains(keyword)
                            || (r.getDescription() != null && r.getDescription().toLowerCase().contains(keyword))
                            || (r.getClient() != null && r.getClient().getNomComplet().toLowerCase().contains(keyword));
                    return matchStatut && matchKeyword;
                })
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

