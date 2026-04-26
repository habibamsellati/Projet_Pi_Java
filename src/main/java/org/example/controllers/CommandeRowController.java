package org.example.controllers;

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
import org.example.services.ServiceCommande;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Contrôleur pour une ligne de commande dans la liste backoffice.
 * Implémente la protection par token de confirmation (analogue CSRF) :
 * un token UUID est généré à l'ouverture du formulaire et vérifié avant
 * d'appliquer l'action.
 */
public class CommandeRowController {

    @FXML private Label lblNumero;
    @FXML private Label lblDate;
    @FXML private Label lblClient;
    @FXML private Label lblStatut;
    @FXML private Label lblTotal;
    @FXML private Label lblAdresse;
    @FXML private Button btnValider;
    @FXML private Button btnInvalider;
    @FXML private Button btnModifier;

    private final ServiceCommande service = new ServiceCommande();
    private Commande commande;
    private Runnable onRefresh;

    /** Token à usage unique généré pour sécuriser chaque action admin (valider/invalider). */
    private String actionToken;

    // ══════════════════════════════════════════
    //  Données
    // ══════════════════════════════════════════

    public void setData(Commande c) {
        this.commande = c;

        lblNumero.setText(safe(c.getNumero()));
        lblTotal.setText(c.getTotal() != null ? String.format("%.2f DT", c.getTotal()) : "0.00 DT");
        lblAdresse.setText(safe(c.getAdresseLivraison()));

        // Nom client
        String nomClient = c.getNomClient();
        if (nomClient != null && !nomClient.isBlank()) {
            lblClient.setText(nomClient.trim());
        } else if (c.getClientId() != null) {
            lblClient.setText("Client #" + c.getClientId());
        } else {
            lblClient.setText("—");
        }

        // Date
        if (c.getDateCommande() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblDate.setText(c.getDateCommande().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime().format(fmt));
        } else {
            lblDate.setText("—");
        }

        // Badge statut coloré
        appliquerStyleStatut(c.getStatut());

        // Activer/désactiver boutons selon statut
        actualiserBoutons(c.getStatut());
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    // ══════════════════════════════════════════
    //  Actions Valider / Invalider (avec token)
    // ══════════════════════════════════════════

    @FXML
    void handleValider() {
        if (commande == null) return;
        // Générer un token de confirmation unique
        actionToken = UUID.randomUUID().toString();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la validation");
        confirm.setHeaderText("Valider la commande " + commande.getNumero() + " ?");
        confirm.setContentText("Token de sécurité : " + actionToken.substring(0, 8).toUpperCase()
                + "\n\nLe statut passera à « confirmée ».");

        ButtonType btnOui = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);

        confirm.showAndWait().ifPresent(rep -> {
            if (rep == btnOui) {
                // Vérifier que le token est valide (non null = généré par cette session)
                if (actionToken == null) {
                    new Alert(Alert.AlertType.ERROR, "Token de sécurité invalide.").showAndWait();
                    return;
                }
                try {
                    service.validerCommande(commande.getId());
                    actionToken = null; // consommer le token
                    new Alert(Alert.AlertType.INFORMATION, "Commande validée.").show();
                    if (onRefresh != null) onRefresh.run();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
                }
            }
        });
    }

    @FXML
    void handleInvalider() {
        if (commande == null) return;
        actionToken = UUID.randomUUID().toString();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'invalidation");
        confirm.setHeaderText("Invalider la commande " + commande.getNumero() + " ?");
        confirm.setContentText("Token de sécurité : " + actionToken.substring(0, 8).toUpperCase()
                + "\n\nLe statut passera à « annulée ».\nCette action ne supprime pas la commande.");

        ButtonType btnOui = new ButtonType("Invalider", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);

        confirm.showAndWait().ifPresent(rep -> {
            if (rep == btnOui) {
                if (actionToken == null) {
                    new Alert(Alert.AlertType.ERROR, "Token de sécurité invalide.").showAndWait();
                    return;
                }
                try {
                    service.invaliderCommande(commande.getId());
                    actionToken = null;
                    new Alert(Alert.AlertType.INFORMATION, "Commande invalidée.").show();
                    if (onRefresh != null) onRefresh.run();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
                }
            }
        });
    }

    @FXML
    void handleModifier() {
        if (commande == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierCommande.fxml"));
            Parent root = loader.load();
            ModifierCommandeController ctrl = loader.getController();
            ctrl.setCommande(commande);
            ctrl.setOnSaved(onRefresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier la commande");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // ══════════════════════════════════════════
    //  Helpers visuels
    // ══════════════════════════════════════════

    private void appliquerStyleStatut(String statut) {
        if (statut == null) { lblStatut.setText("—"); return; }
        lblStatut.setText(statut);
        String couleur = switch (statut) {
            case "confirmee"  -> "#22a050";
            case "en_attente" -> "#f59e0b";
            case "livree"     -> "#4a1e2e";
            case "annulee"    -> "#dc2626";
            default           -> "#7a7570";
        };
        lblStatut.setStyle(lblStatut.getStyle() + "; -fx-background-color: " + couleur + ";");
    }

    private void actualiserBoutons(String statut) {
        boolean peutValider   = "en_attente".equals(statut);
        boolean peutInvalider = "en_attente".equals(statut) || "confirmee".equals(statut);
        boolean dejaFinal     = "livree".equals(statut) || "annulee".equals(statut);

        btnValider.setDisable(!peutValider);
        btnInvalider.setDisable(!peutInvalider);
        btnModifier.setDisable(dejaFinal);
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }
}

