package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.models.Proposition;
import org.example.services.ServiceProposition;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminPropositionsController {

    @FXML private TextField  tfRechercheTop;
    @FXML private VBox       tableContainer;
    @FXML private Label      lblVide;

    private final ServiceProposition service = new ServiceProposition();
    private List<Proposition> all = new ArrayList<>();
    private boolean triParDate = false;

    @FXML
    public void initialize() {
        if (tfRechercheTop != null) {
            tfRechercheTop.textProperty().addListener((obs, o, n) -> filtrer(n));
        }
        chargerTout();
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerTout() {
        try {
            all = service.afficher();
            afficherTableau(all);
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    private void filtrer(String q) {
        if (q == null || q.isBlank()) {
            afficherTableau(all);
            return;
        }
        String low = q.trim().toLowerCase(Locale.ROOT);
        List<Proposition> filtered = all.stream()
                .filter(p -> nvl(p.getTitreAffichage(), "").toLowerCase().contains(low)
                        || nvl(p.getNomProduit(), "").toLowerCase().contains(low)
                        || nvl(p.getDescription(), "").toLowerCase().contains(low))
                .toList();
        afficherTableau(filtered);
    }

    private void afficherTableau(List<Proposition> liste) {
        tableContainer.getChildren().clear();

        if (liste.isEmpty()) {
            lblVide.setVisible(true);
            lblVide.setManaged(true);
            return;
        }
        lblVide.setVisible(false);
        lblVide.setManaged(false);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 0; i < liste.size(); i++) {
            Proposition p = liste.get(i);
            String bg = i % 2 == 0 ? "white" : "#faf8f5";

            GridPane row = new GridPane();
            row.setStyle("-fx-background-color:" + bg + "; -fx-padding: 12 16;");
            row.setAlignment(Pos.CENTER_LEFT);

            // Colonnes identiques à l'en-tête
            for (int c = 0; c < 6; c++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(c == 0 ? 22 : c == 1 ? 28 : c == 2 ? 12 : c == 3 ? 14 : c == 4 ? 12 : 12);
                cc.setHgrow(Priority.ALWAYS);
                row.getColumnConstraints().add(cc);
            }

            // Titre (gras)
            Label lblTitre = new Label(nvl(p.getTitreAffichage(), "—"));
            lblTitre.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #2f2430;");
            lblTitre.setWrapText(true);

            // Description courte
            Label lblDesc = new Label(nvl(p.getDescriptionCourte(), "—"));
            lblDesc.setStyle("-fx-font-size: 12; -fx-text-fill: #7a6a5a;");
            lblDesc.setWrapText(true);

            // Date
            String dateStr = p.getDateCreation() != null
                    ? p.getDateCreation().toLocalDateTime().format(fmt) : "—";
            Label lblDate = new Label(dateStr);
            lblDate.setStyle("-fx-font-size: 12; -fx-text-fill: #5D4037;");

            // Produit
            Label lblProduit = new Label(nvl(p.getNomProduit(), "—"));
            lblProduit.setStyle("-fx-font-size: 12; -fx-text-fill: #5D4037;");

            // Badge statut
            Label lblStatut = new Label(nvl(p.getStatut(), "—"));
            lblStatut.setStyle("-fx-background-color:" + bgStatut(p.getStatut()) + ";" +
                               "-fx-text-fill:" + fgStatut(p.getStatut()) + ";" +
                               "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 11;");

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);
            Button btnEdit = new Button("✎");
            btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #7a5c3a;" +
                             "-fx-font-size: 15; -fx-cursor: hand; -fx-border-color: #dcd5ce;" +
                             "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 8;");
            btnEdit.setOnAction(e -> ouvrirEdition(p));

            Button btnDel = new Button("🗑");
            btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #d93025;" +
                            "-fx-font-size: 14; -fx-cursor: hand; -fx-border-color: #f5c6c6;" +
                            "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 8;");
            btnDel.setOnAction(e -> confirmerSuppression(p));
            actions.getChildren().addAll(btnEdit, btnDel);

            row.add(lblTitre,   0, 0);
            row.add(lblDesc,    1, 0);
            row.add(lblDate,    2, 0);
            row.add(lblProduit, 3, 0);
            row.add(lblStatut,  4, 0);
            row.add(actions,    5, 0);

            // Séparateur
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #fff3e0; -fx-padding: 12 16;"));
            row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:" + bg + "; -fx-padding: 12 16;"));

            tableContainer.getChildren().add(row);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    void handleRechercher() {
        String q = tfRechercheTop != null ? tfRechercheTop.getText() : "";
        filtrer(q);
    }

    @FXML
    void handleReinitialiser() {
        if (tfRechercheTop != null) tfRechercheTop.clear();
        triParDate = false;
        afficherTableau(all);
    }

    @FXML
    void handleTriDate() {
        triParDate = !triParDate;
        List<Proposition> sorted = new ArrayList<>(all);
        sorted.sort(triParDate
                ? Comparator.comparing(Proposition::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(Proposition::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())));
        afficherTableau(sorted);
    }

    @FXML
    void handleStatistiques() {
        long enAttente = all.stream().filter(p -> "en_attente".equals(p.getStatut())).count();
        long acceptees = all.stream().filter(p -> "acceptee".equals(p.getStatut())).count();
        long refusees  = all.stream().filter(p -> "refusee".equals(p.getStatut())).count();
        long terminees = all.stream().filter(p -> "terminee".equals(p.getStatut())).count();
        double prixMoyen = all.stream().mapToDouble(Proposition::getPrixPropose).average().orElse(0);

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Statistiques des propositions");
        a.setHeaderText("Résumé");
        a.setContentText(
            "Total : " + all.size() + "\n" +
            "En attente : " + enAttente + "\n" +
            "Acceptées : " + acceptees + "\n" +
            "Refusées : " + refusees + "\n" +
            "Terminées : " + terminees + "\n" +
            "Prix moyen : " + String.format("%.2f TND", prixMoyen)
        );
        a.showAndWait();
    }

    @FXML
    void handleExporterPdf() {
        showInfo("Export PDF", "Export de " + all.size() + " propositions en cours...\n(Fonctionnalité disponible avec PDFBox)");
    }

    private void ouvrirEdition(Proposition p) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/AjouterProposition.fxml"));
            javafx.scene.Parent root = loader.load();
            AjouterPropositionController ctrl = loader.getController();
            ctrl.setPropositionEnEdition(p);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifier proposition #" + p.getId());
            stage.setScene(new javafx.scene.Scene(root, 780, 700));
            stage.setOnHidden(e -> chargerTout());
            stage.show();
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    private void confirmerSuppression(Proposition p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Suppression"); a.setHeaderText("Supprimer cette proposition ?");
        a.setContentText(p.getTitreAffichage());
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { service.supprimer(p.getId()); chargerTout(); }
        catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String bgStatut(String s) {
        if (s == null) return "#f0f0f0";
        return switch (s) {
            case "acceptee"    -> "#e6f4ea";
            case "refusee"     -> "#fce8e6";
            case "terminee"    -> "#f3e8d8";
            case "en_revision" -> "#e8f0fe";
            default            -> "#fff8e1";
        };
    }

    private String fgStatut(String s) {
        if (s == null) return "#666";
        return switch (s) {
            case "acceptee"    -> "#1e8e3e";
            case "refusee"     -> "#d93025";
            case "terminee"    -> "#7a5c3a";
            case "en_revision" -> "#1565c0";
            default            -> "#F57F17";
        };
    }

    private String nvl(String v, String d) { return (v == null || v.isBlank()) ? d : v; }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    // ── Navigation sidebar ────────────────────────────────────────────────────

    @FXML
    void handleNavDashboard(javafx.event.ActionEvent event) {
        try { org.example.utils.SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art – Backoffice Admin", 1200, 780); }
        catch (Exception e) { showError("Navigation", e.getMessage()); }
    }

    @FXML
    void handleNavProduits(javafx.event.ActionEvent event) {
        try { org.example.utils.SceneNavigator.navigate(event, "/fxml/ProduitsView.fxml", "Backoffice - Produits", 1200, 780); }
        catch (Exception e) { showError("Navigation", e.getMessage()); }
    }

    @FXML
    void handleLogout(javafx.event.ActionEvent event) {
        try {
            org.example.utils.SessionManager.logout();
            org.example.utils.SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception e) { showError("Déconnexion", e.getMessage()); }
    }
}
