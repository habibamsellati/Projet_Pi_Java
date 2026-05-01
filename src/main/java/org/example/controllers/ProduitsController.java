package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.example.models.Produit;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceProduit;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProduitsController {

    // Champs du nouveau FXML (catalogue uniquement)
    @FXML private TextField         tfRecherche;
    @FXML private ComboBox<String>  cbFiltreEtat;
    @FXML private ComboBox<String>  cbTri;
    @FXML private VBox              produitsContainer;
    @FXML private Label             lblAucunProduit;

    private final ServiceProduit    serviceProduit = new ServiceProduit();
    private final List<Produit>     allProduits    = new ArrayList<>();

    @FXML
    void initialize() {
        cbFiltreEtat.getItems().setAll("Tous les états", "Bon", "Moyen","mauvais" );
        cbTri.getItems().setAll("Plus récents", "Nom (A-Z)", "Impact écologique", "Quantité disponible");
        cbFiltreEtat.setValue("Tous les états");
        cbTri.setValue("Plus récents");

        tfRecherche.textProperty().addListener((obs, o, n) -> appliquerFiltres());
        cbFiltreEtat.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
        cbTri.valueProperty().addListener((obs, o, n) -> appliquerFiltres());

        rafraichirListe();
    }

    // ── Actions FXML ─────────────────────────────────────────────────────────

    @FXML
    void handleAfficherFormulaire() {
        User u = SessionManager.getCurrentUser();
        if (u != null && u.getRole() == Role.ARTISANT) {
            showError("Accès refusé", "Les artisans ne peuvent que consulter les produits.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterProduit.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Ajouter un produit recyclable");
            stage.setScene(new javafx.scene.Scene(root, 780, 700));
            stage.setOnHidden(e -> rafraichirListe());
            stage.show();
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    void handleRafraichir() {
        rafraichirListe();
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            User u = SessionManager.getCurrentUser();
            if (u == null) {
                SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
                return;
            }
            if (u.getRole() == Role.ADMIN) {
                SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art", 1200, 780);
            } else {
                SceneNavigator.navigate(event, "/fxml/MainView.fxml", "Artefact", 1200, 750);
            }
        } catch (Exception e) {
            showError("Navigation", e.getMessage());
        }
    }

    // ── Chargement et affichage ───────────────────────────────────────────────

    private void rafraichirListe() {
        try {
            User u = SessionManager.getCurrentUser();
            allProduits.clear();
            if (u != null && u.getRole() == Role.ARTISANT) {
                allProduits.addAll(serviceProduit.afficherParArtisan(u.getId()));
            } else {
                allProduits.addAll(serviceProduit.afficher());
            }
            appliquerFiltres();
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    private void appliquerFiltres() {
        produitsContainer.getChildren().clear();

        String recherche = tfRecherche.getText() == null ? "" : tfRecherche.getText().trim().toLowerCase(Locale.ROOT);
        String etat      = cbFiltreEtat.getValue();
        User u = SessionManager.getCurrentUser();

        List<Produit> filtered = allProduits.stream()
                .filter(p -> recherche.isBlank() || matchRecherche(p, recherche))
                .filter(p -> etat == null || "Tous les états".equals(etat) || etat.equalsIgnoreCase(safe(p.getEtat())))
                .sorted(buildComparator())
                .toList();

        // Grouper par nom (catégorie)
        Map<String, List<Produit>> parCat = new LinkedHashMap<>();
        for (Produit p : filtered) {
            String cat = safe(p.getNom()).isBlank() ? "Autres" : p.getNom();
            parCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<Produit>> entry : parCat.entrySet()) {
            // Titre catégorie
            Label lblCat = new Label("Catégorie : " + entry.getKey());
            lblCat.setStyle("-fx-font-family: Georgia; -fx-font-size: 22; -fx-text-fill: #2f2430; -fx-padding: 0 0 8 0;");
            produitsContainer.getChildren().add(lblCat);

            // Grille FlowPane
            FlowPane grille = new FlowPane();
            grille.setHgap(20);
            grille.setVgap(20);
            grille.setPrefWrapLength(980);

            for (Produit produit : entry.getValue()) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProduitCard.fxml"));
                    javafx.scene.Node card = loader.load();
                    ProduitCardController ctrl = loader.getController();
                    boolean canEdit = u == null || (u.getRole() != Role.ARTISANT);
                    ctrl.setData(produit, canEdit, this::ouvrirEdition, this::confirmerSuppression);
                    grille.getChildren().add(card);
                } catch (Exception e) {
                    showError("Erreur UI", e.getMessage());
                    break;
                }
            }
            produitsContainer.getChildren().add(grille);
        }

        boolean empty = filtered.isEmpty();
        lblAucunProduit.setVisible(empty);
        lblAucunProduit.setManaged(empty);
    }

    // ── Actions sur les cartes ────────────────────────────────────────────────

    private void ouvrirEdition(Produit produit) {
        User u = SessionManager.getCurrentUser();
        if (u != null && u.getRole() == Role.ARTISANT) {
            showError("Accès refusé", "Les artisans ne peuvent pas modifier de produits.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterProduit.fxml"));
            javafx.scene.Parent root = loader.load();
            AjouterProduitController ctrl = loader.getController();
            ctrl.setProduitEnEdition(produit);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifier le produit #" + produit.getId());
            stage.setScene(new javafx.scene.Scene(root, 780, 700));
            stage.setOnHidden(e -> rafraichirListe());
            stage.show();
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void confirmerSuppression(Produit produit) {
        User u = SessionManager.getCurrentUser();
        if (u != null && u.getRole() == Role.ARTISANT) {
            showError("Accès refusé", "Les artisans ne peuvent pas supprimer de produits.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer ce produit recyclable ?");
        alert.setContentText(produit.getNomAffichage());
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            serviceProduit.supprimer(produit.getId());
            rafraichirListe();
            showInfo("Suppression", "Produit supprimé.");
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private Comparator<Produit> buildComparator() {
        String tri = cbTri.getValue();
        if ("Nom (A-Z)".equals(tri))
            return Comparator.comparing(p -> safe(p.getNom()).toLowerCase(Locale.ROOT));
        if ("Impact écologique".equals(tri))
            return Comparator.comparingInt(Produit::getImpactEcologique).reversed();
        if ("Quantité disponible".equals(tri))
            return Comparator.comparingInt(Produit::getQuantite).reversed();
        return Comparator
                .comparing(Produit::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Produit::getId, Comparator.reverseOrder());
    }

    private boolean matchRecherche(Produit p, String q) {
        return safe(p.getNom()).toLowerCase(Locale.ROOT).contains(q)
                || safe(p.getTypeMateriau()).toLowerCase(Locale.ROOT).contains(q)
                || safe(p.getEtat()).toLowerCase(Locale.ROOT).contains(q)
                || safe(p.getOrigine()).toLowerCase(Locale.ROOT).contains(q)
                || safe(p.getDescription()).toLowerCase(Locale.ROOT).contains(q);
    }

    private String safe(String v) { return v == null ? "" : v; }

    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
