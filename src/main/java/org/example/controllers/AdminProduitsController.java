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
import org.example.models.Produit;
import org.example.services.ServiceProduit;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminProduitsController {

    @FXML private TextField tfRechercheTop;
    @FXML private VBox      tableContainer;
    @FXML private Label     lblVide;

    private final ServiceProduit service = new ServiceProduit();
    private List<Produit> all = new ArrayList<>();
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
        if (q == null || q.isBlank()) { afficherTableau(all); return; }
        String low = q.trim().toLowerCase(Locale.ROOT);
        List<Produit> filtered = all.stream()
                .filter(p -> nvl(p.getNom(), "").toLowerCase().contains(low)
                        || nvl(p.getTypeMateriau(), "").toLowerCase().contains(low)
                        || nvl(p.getEtat(), "").toLowerCase().contains(low)
                        || nvl(p.getOrigine(), "").toLowerCase().contains(low)
                        || nvl(p.getDescription(), "").toLowerCase().contains(low))
                .toList();
        afficherTableau(filtered);
    }

    // ── Affichage tableau ─────────────────────────────────────────────────────

    private void afficherTableau(List<Produit> liste) {
        tableContainer.getChildren().clear();

        if (liste.isEmpty()) {
            lblVide.setVisible(true); lblVide.setManaged(true); return;
        }
        lblVide.setVisible(false); lblVide.setManaged(false);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 0; i < liste.size(); i++) {
            Produit p = liste.get(i);
            String bg = i % 2 == 0 ? "white" : "#faf8f5";

            GridPane row = new GridPane();
            row.setStyle("-fx-background-color:" + bg + "; -fx-padding: 11 16;");
            row.setAlignment(Pos.CENTER_LEFT);

            for (int c = 0; c < 7; c++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(c == 0 ? 20 : c == 1 ? 15 : c == 2 ? 12 : c == 3 ? 10 : c == 4 ? 15 : c == 5 ? 15 : 13);
                cc.setHgrow(Priority.ALWAYS);
                row.getColumnConstraints().add(cc);
            }

            // Nom produit (gras)
            Label lblNom = new Label(nvl(p.getNomAffichage(), "—"));
            lblNom.setStyle("-fx-font-weight:bold;-fx-font-size:13;-fx-text-fill:#2f2430;");

            // Type matériau
            Label lblType = new Label(nvl(p.getTypeMateriau(), "—"));
            lblType.setStyle("-fx-font-size:12;-fx-text-fill:#5D4037;");

            // Badge état
            Label lblEtat = new Label(nvl(p.getEtat(), "—"));
            lblEtat.setStyle("-fx-background-color:" + bgEtat(p.getEtat()) + ";" +
                             "-fx-text-fill:" + fgEtat(p.getEtat()) + ";" +
                             "-fx-background-radius:6;-fx-padding:3 8;-fx-font-size:11;");

            // Quantité
            Label lblQte = new Label(String.valueOf(p.getQuantite()));
            lblQte.setStyle("-fx-font-size:12;-fx-text-fill:#5D4037;");

            // Origine
            Label lblOrigine = new Label(nvl(p.getOrigine(), "—"));
            lblOrigine.setStyle("-fx-font-size:12;-fx-text-fill:#5D4037;");

            // Date ajout
            String dateStr = p.getDateCreation() != null
                    ? p.getDateCreation().toLocalDateTime().format(fmt) : "—";
            Label lblDate = new Label(dateStr);
            lblDate.setStyle("-fx-font-size:12;-fx-text-fill:#9a9a9a;");

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);

            Button btnEdit = new Button("✎");
            btnEdit.setStyle("-fx-background-color:transparent;-fx-text-fill:#7a5c3a;" +
                             "-fx-font-size:15;-fx-cursor:hand;-fx-border-color:#dcd5ce;" +
                             "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:3 8;");
            btnEdit.setOnAction(e -> ouvrirEdition(p));

            Button btnDel = new Button("🗑");
            btnDel.setStyle("-fx-background-color:transparent;-fx-text-fill:#d93025;" +
                            "-fx-font-size:14;-fx-cursor:hand;-fx-border-color:#f5c6c6;" +
                            "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:3 8;");
            btnDel.setOnAction(e -> confirmerSuppression(p));
            actions.getChildren().addAll(btnEdit, btnDel);

            row.add(lblNom,     0, 0);
            row.add(lblType,    1, 0);
            row.add(lblEtat,    2, 0);
            row.add(lblQte,     3, 0);
            row.add(lblOrigine, 4, 0);
            row.add(lblDate,    5, 0);
            row.add(actions,    6, 0);

            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#fff3e0;-fx-padding:11 16;"));
            row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:" + bg + ";-fx-padding:11 16;"));

            tableContainer.getChildren().add(row);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML void handleReinitialiser() {
        if (tfRechercheTop != null) tfRechercheTop.clear();
        triParDate = false;
        afficherTableau(all);
    }

    @FXML void handleTriDate() {
        triParDate = !triParDate;
        List<Produit> sorted = new ArrayList<>(all);
        sorted.sort(triParDate
                ? Comparator.comparing(Produit::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(Produit::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())));
        afficherTableau(sorted);
    }

    @FXML void handleStatistiques() {
        long excellent = all.stream().filter(p -> "Excellent".equalsIgnoreCase(p.getEtat())).count();
        long bon       = all.stream().filter(p -> "Bon".equalsIgnoreCase(p.getEtat())).count();
        long moyen     = all.stream().filter(p -> "Moyen".equalsIgnoreCase(p.getEtat())).count();
        long recycler  = all.stream().filter(p -> "A recycler".equalsIgnoreCase(p.getEtat())).count();
        int totalQte   = all.stream().mapToInt(Produit::getQuantite).sum();

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Statistiques des produits");
        a.setHeaderText("Résumé");
        a.setContentText(
            "Total produits : " + all.size() + "\n" +
            "Quantité totale : " + totalQte + " unités\n\n" +
            "Par état :\n" +
            "  Excellent : " + excellent + "\n" +
            "  Bon : " + bon + "\n" +
            "  Moyen : " + moyen + "\n" +
            "  À recycler : " + recycler
        );
        a.showAndWait();
    }

    @FXML void handleAjouter() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/AjouterProduit.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Ajouter un produit recyclable");
            stage.setScene(new javafx.scene.Scene(root, 780, 700));
            stage.setOnHidden(e -> chargerTout());
            stage.show();
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    private void ouvrirEdition(Produit p) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/AjouterProduit.fxml"));
            javafx.scene.Parent root = loader.load();
            AjouterProduitController ctrl = loader.getController();
            ctrl.setProduitEnEdition(p);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifier produit #" + p.getId());
            stage.setScene(new javafx.scene.Scene(root, 780, 700));
            stage.setOnHidden(e -> chargerTout());
            stage.show();
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    private void confirmerSuppression(Produit p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Suppression"); a.setHeaderText("Supprimer ce produit ?");
        a.setContentText(p.getNomAffichage());
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { service.supprimer(p.getId()); chargerTout(); }
        catch (IllegalArgumentException ex) { showError("Impossible", ex.getMessage()); }
        catch (SQLException ex) { showError("Erreur SQL", ex.getMessage()); }
    }

    // ── Navigation sidebar ────────────────────────────────────────────────────

    @FXML void handleNavDashboard(javafx.event.ActionEvent event) {
        try { org.example.utils.SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art – Backoffice Admin", 1200, 780); }
        catch (Exception e) { showError("Navigation", e.getMessage()); }
    }

    @FXML void handleNavPropositions(javafx.event.ActionEvent event) {
        try { org.example.utils.SceneNavigator.navigate(event, "/fxml/AdminPropositionsView.fxml", "Backoffice - Propositions", 1200, 780); }
        catch (Exception e) { showError("Navigation", e.getMessage()); }
    }

    @FXML void handleLogout(javafx.event.ActionEvent event) {
        try {
            org.example.utils.SessionManager.logout();
            org.example.utils.SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception e) { showError("Déconnexion", e.getMessage()); }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String bgEtat(String e) {
        if (e == null) return "#f0f0f0";
        return switch (e.toLowerCase()) {
            case "excellent"  -> "#e6f4ea";
            case "bon"        -> "#e8f5e9";
            case "moyen"      -> "#fff8e1";
            case "a recycler" -> "#fce8e6";
            default           -> "#f0f0f0";
        };
    }

    private String fgEtat(String e) {
        if (e == null) return "#666";
        return switch (e.toLowerCase()) {
            case "excellent"  -> "#1e8e3e";
            case "bon"        -> "#2e7d32";
            case "moyen"      -> "#F57F17";
            case "a recycler" -> "#d93025";
            default           -> "#666";
        };
    }

    private String nvl(String v, String d) { return (v == null || v.isBlank()) ? d : v; }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
