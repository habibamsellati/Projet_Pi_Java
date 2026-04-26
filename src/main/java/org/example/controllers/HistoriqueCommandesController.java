package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.models.Commande;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class HistoriqueCommandesController {

    @FXML private VBox  vbCommandes;
    @FXML private VBox  vboxEmpty;
    @FXML private Label lblCount;

    private final ServiceCommande serviceCommande = new ServiceCommande();

    @FXML
    void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showError("Vous n'êtes pas connecté.");
            return;
        }
        try {
            List<Commande> commandes = serviceCommande.afficherParClient(user.getId());
            if (commandes.isEmpty()) {
                vboxEmpty.setVisible(true);
                vboxEmpty.setManaged(true);
                lblCount.setText("0 commande");
            } else {
                lblCount.setText(commandes.size() + " commande" + (commandes.size() > 1 ? "s" : ""));
                for (Commande c : commandes) {
                    vbCommandes.getChildren().add(buildCommandeCard(c));
                }
            }
        } catch (Exception e) {
            showError("Erreur chargement historique : " + e.getMessage());
        }
    }

    @FXML
    void handleRetour() {
        navigate("/fxml/ClientDashboard.fxml", "Espace Client", 900, 620);
    }

    @FXML
    void handleVoirArticles() {
        navigate("/fxml/ClientCatalogue.fxml", "Catalogue", 1320, 860);
    }

    // ── Construction de la carte commande ─────────────────────────────────

    private VBox buildCommandeCard(Commande commande) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.09), 14, 0, 0, 4);");

        // ── Ligne 1 : numéro + badge statut ──────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblNumero = new Label("# " + (commande.getNumero() != null ? commande.getNumero() : "—"));
        lblNumero.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #5D4037;");
        HBox.setHgrow(lblNumero, Priority.ALWAYS);

        Label badge = buildStatutBadge(commande.getStatut());

        header.getChildren().addAll(lblNumero, badge);
        card.getChildren().add(header);

        // ── Séparateur ─────────────────────────────────────────────────
        Separator sep = new Separator();
        card.getChildren().add(sep);

        // ── Ligne 2 : Date + Téléphone ────────────────────────────────
        HBox row2 = new HBox(24);
        row2.setAlignment(Pos.CENTER_LEFT);

        String dateStr = "—";
        if (commande.getDateCommande() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy à HH:mm");
            dateStr = commande.getDateCommande().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime().format(fmt);
        }
        Label lblDate = new Label("📅 " + dateStr);
        lblDate.setStyle("-fx-font-size: 13; -fx-text-fill: #A1887F;");

        String tel = commande.getTelephone() != null && !commande.getTelephone().isBlank()
                ? commande.getTelephone() : "Non renseigné";
        Label lblTel = new Label("📞 " + tel);
        lblTel.setStyle("-fx-font-size: 13; -fx-text-fill: #A1887F;");

        row2.getChildren().addAll(lblDate, lblTel);
        card.getChildren().add(row2);

        // ── Ligne 3 : Articles ────────────────────────────────────────
        if (commande.getArticles() != null && !commande.getArticles().isEmpty()) {
            String articlesStr = commande.getArticles().stream()
                    .map(Article::getTitre)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining(" · "));
            Label lblArticles = new Label("🛍 Articles : " + articlesStr);
            lblArticles.setStyle("-fx-font-size: 14; -fx-text-fill: #6D4C41;");
            lblArticles.setWrapText(true);
            card.getChildren().add(lblArticles);
        }

        // ── Ligne 4 : Adresse ─────────────────────────────────────────
        if (commande.getAdresseLivraison() != null && !commande.getAdresseLivraison().isBlank()) {
            Label lblAdresse = new Label("📍 " + commande.getAdresseLivraison());
            lblAdresse.setStyle("-fx-font-size: 13; -fx-text-fill: #8D6E63;");
            lblAdresse.setWrapText(true);
            card.getChildren().add(lblAdresse);
        }

        // ── Ligne 5 : Total ───────────────────────────────────────────
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Label lblTotal = new Label(commande.getTotal() != null
                ? String.format("Total : %.2f DT", commande.getTotal())
                : "Total : — DT");
        lblTotal.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2E8B57;");
        footer.getChildren().add(lblTotal);
        card.getChildren().add(footer);

        return card;
    }

    // ── Badge selon le statut ──────────────────────────────────────────────

    private Label buildStatutBadge(String statut) {
        Label badge = new Label();
        if ("en_attente".equalsIgnoreCase(statut) || statut == null) {
            badge.setText("EN ATTENTE");
            badge.setStyle("-fx-background-color: #F0C040; -fx-text-fill: #7A5500; " +
                           "-fx-font-weight: bold; -fx-font-size: 12; " +
                           "-fx-background-radius: 14; -fx-padding: 5 14;");
        } else if ("confirmee".equalsIgnoreCase(statut)) {
            badge.setText("CONFIRMÉE");
            badge.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                           "-fx-font-weight: bold; -fx-font-size: 12; " +
                           "-fx-background-radius: 14; -fx-padding: 5 14;");
        } else if ("livree".equalsIgnoreCase(statut)) {
            badge.setText("LIVRÉE");
            badge.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                           "-fx-font-weight: bold; -fx-font-size: 12; " +
                           "-fx-background-radius: 14; -fx-padding: 5 14;");
        } else {
            badge.setText(statut.toUpperCase());
            badge.setStyle("-fx-background-color: #BDBDBD; -fx-text-fill: white; " +
                           "-fx-font-weight: bold; -fx-font-size: 12; " +
                           "-fx-background-radius: 14; -fx-padding: 5 14;");
        }
        return badge;
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private void navigate(String fxml, String title, double w, double h) {
        try {
            Stage stage = (Stage) vbCommandes.getScene().getWindow();
            SceneNavigator.navigate(stage, fxml, title, w, h);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}

