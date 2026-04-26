package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label userNomLabel;
    @FXML private Label userRoleLabel;

    @FXML private Button btnAccueil;
    @FXML private Button btnBlog;
    @FXML private Button btnLivraisons;
    @FXML private Button btnSuiviLivraison;
    @FXML private Button btnProduits;
    @FXML private Button btnPropositions;
    @FXML private Button btnEvenements;
    @FXML private Button btnReservations;
    @FXML private Button btnReclamations;

    @FXML
    void initialize() {
        if (SessionManager.utilisateurConnecte != null) {
            userNomLabel.setText(
                    SessionManager.utilisateurConnecte.prenom + " " + SessionManager.utilisateurConnecte.nom
            );
            userRoleLabel.setText("(" + SessionManager.utilisateurConnecte.role + ")");
        }

        chargerPage("/fxml/AccueilView.fxml");
        setActif(btnAccueil);
    }

    @FXML public void goAccueil() { chargerPage("/fxml/AccueilView.fxml"); setActif(btnAccueil); }
    @FXML public void goBlog() { chargerPage("/fxml/BlogView.fxml"); setActif(btnBlog); }
    @FXML public void goLivraisons() { chargerPage("/fxml/LivraisonsView.fxml"); setActif(btnLivraisons); }
    @FXML public void goSuiviLivraison() { chargerPage("/fxml/SuiviLivraisonView.fxml"); setActif(btnSuiviLivraison); }
    @FXML public void goProduits() { chargerPage("/fxml/ProduitsView.fxml"); setActif(btnProduits); }
    @FXML public void goPropositions() { chargerPage("/fxml/PropositionsView.fxml"); setActif(btnPropositions); }
    @FXML public void goEvenements() { chargerPage("/fxml/EvenementsView.fxml"); setActif(btnEvenements); }
    @FXML public void goReservations() { chargerPage("/fxml/ReservationsView.fxml"); setActif(btnReservations); }
    @FXML public void goReclamations() { chargerPage("/fxml/ReclamationsView.fxml"); setActif(btnReclamations); }

    private void chargerPage(String fxmlPath) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            e.printStackTrace();
            Label placeholder = new Label("Erreur de chargement : " + fxmlPath + "\n" + (e.getMessage() == null ? "Cause inconnue" : e.getMessage()));
            placeholder.setStyle("-fx-font-size:16px; -fx-text-fill:#999; -fx-font-family:Georgia;");
            contentArea.getChildren().setAll(placeholder);
        }
    }

    private void setActif(Button actif) {
        Button[] tous = {
                btnAccueil, btnBlog, btnLivraisons, btnSuiviLivraison,
                btnProduits, btnPropositions, btnEvenements,
                btnReservations, btnReclamations
        };
        for (Button b : tous) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-btn-active");
            if (!b.getStyleClass().contains("nav-btn")) {
                b.getStyleClass().add("nav-btn");
            }
        }
        if (actif != null) {
            actif.getStyleClass().remove("nav-btn");
            actif.getStyleClass().add("nav-btn-active");
        }
    }

    @FXML
    private void handleDeconnexion() {
        try {
            SessionManager.utilisateurConnecte = null;
            org.example.utils.SessionManager.logout();
            Parent login = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(login, 520, 420));
            stage.setTitle("Artefact - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}

