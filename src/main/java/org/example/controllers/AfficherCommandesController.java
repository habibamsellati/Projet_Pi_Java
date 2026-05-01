package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.models.Commande;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class AfficherCommandesController {
    @FXML private VBox commandesContainer;

    private final ServiceCommande service = new ServiceCommande();

    @FXML
    void initialize() {
        rafraichirListe();
    }

    @FXML
    void handleRafraichir() {
        rafraichirListe();
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            User user = SessionManager.getCurrentUser();
            if (user != null && user.getRole() == Role.ADMIN) {
                SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art – Backoffice Admin", 1200, 780);
            } else {
                SceneNavigator.navigate(event, "/fxml/MainView.fxml", "Artefact", 1200, 750);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleNouvelleCommande() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterCommande.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) commandesContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 760));
            stage.setTitle("Artefact - Ajouter une Commande");
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void rafraichirListe() {
        commandesContainer.getChildren().clear();
        try {
            List<Commande> commandes = service.afficher();
            for (Commande commande : commandes) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CommandeCard.fxml"));
                Node node = loader.load();

                CommandeCardController controller = loader.getController();
                controller.setData(commande);
                controller.setOnRefresh(this::rafraichirListe);

                commandesContainer.getChildren().add(node);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur SQL : " + e.getMessage()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur UI : " + e.getMessage()).showAndWait();
        }
    }
}

