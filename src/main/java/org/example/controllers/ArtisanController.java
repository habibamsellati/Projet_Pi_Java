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
import org.example.utils.SessionStore;
import org.example.models.User;

import java.io.IOException;

public class ArtisanController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label lblViewTitle, lblViewSubtitle;
    @FXML
    private Label lblAdminName, lblAdminRole, lblAdminInitials;

    @FXML
    public void initialize() {
        if (SessionStore.isConnected()) {
            User u = SessionStore.getCurrentUser();
            lblAdminName.setText(u.getNom() + " " + u.getPrenom());
            lblAdminRole.setText("Artisan Certifié");

            String initials = "";
            if (u.getNom() != null && !u.getNom().isEmpty())
                initials += u.getNom().charAt(0);
            if (u.getPrenom() != null && !u.getPrenom().isEmpty())
                initials += u.getPrenom().charAt(0);
            lblAdminInitials.setText(initials.toUpperCase());
        }
        showEvenementsView();
    }

    @FXML
    void showEvenementsView() {
        lblViewTitle.setText("Mes Événements");
        lblViewSubtitle.setText("Gérez vos ateliers et expositions");
        loadView("/fxml/AdminEvents.fxml");
    }

    @FXML
    void showReservationsView() {
        lblViewTitle.setText("Mes Réservations");
        lblViewSubtitle.setText("Suivi des participants et validation");
        loadView("/fxml/AdminReservations.fxml");
    }

    @FXML
    void showArticlesView() {
        lblViewTitle.setText("Mon E-commerce");
        lblViewSubtitle.setText("Ventes d'articles et inventaire");
        loadView("/fxml/ModuleEcommerceV1.fxml");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        SessionStore.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
