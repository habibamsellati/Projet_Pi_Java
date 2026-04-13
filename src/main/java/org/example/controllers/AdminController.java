package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminController {

    @FXML private StackPane contentArea;
    @FXML private Label lblViewTitle, lblViewSubtitle;

    @FXML
    public void initialize() {
        showDashboardView();
    }

    @FXML
    void showDashboardView() {
        lblViewTitle.setText("Dashboard");
        lblViewSubtitle.setText("Aperçu de l'activité globale");
        loadView("/fxml/AdminDashboardHome.fxml");
    }

    @FXML
    void showEvenementsView() {
        lblViewTitle.setText("Gestion des Événements");
        lblViewSubtitle.setText("Catalogue complet de vos manifestations culturelles");
        loadView("/fxml/AdminEvents.fxml");
    }

    @FXML
    void showReservationsView() {
        lblViewTitle.setText("Gestion des Réservations");
        lblViewSubtitle.setText("Billets, confirmations et suivi des places");
        loadView("/fxml/AdminReservations.fxml");
    }

    @FXML
    void showCommandesView() {
        lblViewTitle.setText("Gestion des Commandes");
        lblViewSubtitle.setText("Administration des transactions clients");
        loadView("/fxml/AdminCommandes.fxml");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ModuleEvenementReservation.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void unsupportedFeature() {
        System.out.println("Fonctionnalité non implémentée (Articles/Réclamations).");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            contentArea.getChildren().clear();
            Label err = new Label("Erreur de chargement: " + e.getMessage());
            err.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            contentArea.getChildren().add(err);
        }
    }
}
