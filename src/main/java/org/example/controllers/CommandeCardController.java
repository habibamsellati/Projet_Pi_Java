package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.Commande;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CommandeCardController {
    @FXML private Label lblNumero;
    @FXML private Label lblDate;
    @FXML private Label lblStatut;
    @FXML private Label lblTotal;
    @FXML private Label lblAdresse;
    @FXML private Label lblTelephone;
    @FXML private Label lblModePaiement;
    @FXML private Label lblClient;
    @FXML private Button btnSupprimer;

    private final ServiceCommande service = new ServiceCommande();
    private Commande currentCommande;
    private Runnable onRefresh;

    public void setData(Commande commande) {
        currentCommande = commande;
        lblNumero.setText(commande.getNumero());
        lblStatut.setText(commande.getStatut());
        lblTotal.setText(commande.getTotal() == null ? "0.00 DT" : String.format("%.2f DT", commande.getTotal()));
        lblAdresse.setText(commande.getAdresseLivraison());
        lblTelephone.setText(commande.getTelephone() == null || commande.getTelephone().isBlank() ? "Non renseigné" : commande.getTelephone());
        lblModePaiement.setText(commande.getModePaiement());
        lblClient.setText(commande.getClientId() == null ? "Aucun client" : "Client #" + commande.getClientId());

        if (commande.getDateCommande() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblDate.setText(commande.getDateCommande().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter));
        } else {
            lblDate.setText("Date inconnue");
        }

        boolean isAdmin = isAdminUser();
        btnSupprimer.setDisable(!isAdmin || !commande.canBeCancelled());
        if (!isAdmin) {
            btnSupprimer.setText("Admin uniquement");
        } else if (!commande.canBeCancelled()) {
            btnSupprimer.setText("Non annulable");
        } else {
            btnSupprimer.setText("Annuler");
        }
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    @FXML
    void handleModifier(ActionEvent event) {
        if (currentCommande == null) {
            return;
        }
        if (!isAdminUser()) {
            new Alert(Alert.AlertType.WARNING, "Accès refusé : seul un admin peut modifier une commande.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierCommande.fxml"));
            Parent root = loader.load();

            ModifierCommandeController controller = loader.getController();
            controller.setCommande(currentCommande);
            controller.setOnSaved(onRefresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier la commande");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (currentCommande == null) {
            return;
        }
        if (!isAdminUser()) {
            new Alert(Alert.AlertType.WARNING, "Accès refusé : seul un admin peut annuler une commande.").showAndWait();
            return;
        }
        if (!currentCommande.canBeCancelled()) {
            new Alert(Alert.AlertType.WARNING, "Une commande livrée ne peut plus être annulée.").showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Annuler la commande ?");
        alert.setContentText(currentCommande.getNumero());

        ButtonType oui = new ButtonType("Oui, annuler", ButtonBar.ButtonData.OK_DONE);
        ButtonType non = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(oui, non);

        alert.showAndWait().ifPresent(reponse -> {
            if (reponse == oui) {
                try {
                    service.annulerCommande(currentCommande.getId());
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
                }
            }
        });
    }

    private boolean isAdminUser() {
        User user = SessionManager.getCurrentUser();
        return user != null && user.getRole() == Role.ADMIN;
    }
}

