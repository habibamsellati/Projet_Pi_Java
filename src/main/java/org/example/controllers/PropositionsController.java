package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import org.example.models.Proposition;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceProposition;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PropositionsController {

    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbFiltreStatut;
    @FXML private ComboBox<String> cbFiltreProduit;
    @FXML private VBox             propositionsContainer;
    @FXML private Label            lblAucuneProposition;

    // Calendrier
    @FXML private VBox      vboxCalendrier;
    @FXML private GridPane  gridCalendrier;
    @FXML private Label     lblCalMois;
    @FXML private javafx.scene.control.Button btnCalendrier;

    private YearMonth moisCalendrier = YearMonth.now();

    private final ServiceProposition service = new ServiceProposition();
    private final List<Proposition>  all     = new ArrayList<>();

    @FXML
    void initialize() {
        cbFiltreStatut.getItems().setAll("Tous les statuts", "en_attente", "en_revision", "acceptee", "refusee", "terminee");
        cbFiltreStatut.setValue("Tous les statuts");
        cbFiltreProduit.getItems().setAll("Tous les produits");
        cbFiltreProduit.setValue("Tous les produits");

        tfRecherche.textProperty().addListener((o, a, b) -> appliquerFiltres());
        cbFiltreStatut.valueProperty().addListener((o, a, b) -> appliquerFiltres());
        cbFiltreProduit.valueProperty().addListener((o, a, b) -> appliquerFiltres());

        // Bouton calendrier visible uniquement pour ARTISANT et ADMIN
        User u = SessionManager.getCurrentUser();
        boolean estArtisanOuAdmin = u != null &&
                (u.getRole() == Role.ARTISANT || u.getRole() == Role.ADMIN);
        if (btnCalendrier != null) {
            btnCalendrier.setVisible(estArtisanOuAdmin);
            btnCalendrier.setManaged(estArtisanOuAdmin);
        }

        rafraichirListe();
    }

    // ── Calendrier ────────────────────────────────────────────────────────────

    @FXML
    void handleToggleCalendrier() {
        if (vboxCalendrier == null) return;
        boolean visible = !vboxCalendrier.isVisible();
        vboxCalendrier.setVisible(visible);
        vboxCalendrier.setManaged(visible);
        if (visible) rafraichirCalendrier();
    }

    @FXML void handleCalMoisPrecedent() { moisCalendrier = moisCalendrier.minusMonths(1); rafraichirCalendrier(); }
    @FXML void handleCalMoisSuivant()   { moisCalendrier = moisCalendrier.plusMonths(1);  rafraichirCalendrier(); }

    private void rafraichirCalendrier() {
        if (gridCalendrier == null || lblCalMois == null) return;

        // Titre mois
        String nomMois = moisCalendrier.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblCalMois.setText(nomMois + " " + moisCalendrier.getYear());

        // Indexer propositions par date
        Map<LocalDate, List<Proposition>> parDate = new HashMap<>();
        for (Proposition p : all) {
            if (p.getDateCreation() != null) {
                LocalDate date = p.getDateCreation().toLocalDateTime().toLocalDate();
                if (date.getYear() == moisCalendrier.getYear()
                        && date.getMonth() == moisCalendrier.getMonth()) {
                    parDate.computeIfAbsent(date, k -> new ArrayList<>()).add(p);
                }
            }
        }

        gridCalendrier.getChildren().clear();
        gridCalendrier.getColumnConstraints().clear();
        gridCalendrier.getRowConstraints().clear();

        // 7 colonnes égales
        String[] joursSemaine = {"dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."};
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            gridCalendrier.getColumnConstraints().add(cc);
        }

        // En-têtes
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(joursSemaine[i]);
            lbl.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#8a7060;" +
                         "-fx-alignment:CENTER;-fx-padding:4 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            gridCalendrier.add(lbl, i, 0);
        }

        // Jours du mois
        LocalDate premierJour = moisCalendrier.atDay(1);
        // Dimanche = 0, Lundi = 1 ... (format US pour correspondre à la capture)
        int debutCol = premierJour.getDayOfWeek().getValue() % 7; // 0=dim, 1=lun...
        int nbJours = moisCalendrier.lengthOfMonth();
        LocalDate aujourdhui = LocalDate.now();

        int col = debutCol;
        int row = 1;

        // Jours du mois précédent (grisés)
        YearMonth moisPrec = moisCalendrier.minusMonths(1);
        int nbJoursPrec = moisPrec.lengthOfMonth();
        for (int i = debutCol - 1; i >= 0; i--) {
            Label lbl = new Label(String.valueOf(nbJoursPrec - (debutCol - 1 - i)));
            lbl.setStyle("-fx-font-size:12;-fx-text-fill:#c8b8a8;-fx-alignment:CENTER;-fx-padding:6 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            gridCalendrier.add(lbl, i, 1);
        }

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCalendrier.atDay(jour);
            List<Proposition> props = parDate.getOrDefault(date, new ArrayList<>());
            boolean estAujourdhui = date.equals(aujourdhui);

            VBox cell = new VBox(2);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPadding(new Insets(4, 2, 4, 2));
            cell.setMinHeight(60);
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setStyle("-fx-background-color:" + (estAujourdhui ? "#fff3e0" : "white") + ";" +
                          "-fx-background-radius:6;");

            Label lblJour = new Label(String.valueOf(jour));
            lblJour.setStyle("-fx-font-size:" + (estAujourdhui ? "13" : "12") + ";" +
                             "-fx-font-weight:" + (estAujourdhui ? "bold" : "normal") + ";" +
                             "-fx-text-fill:" + (estAujourdhui ? "#7a5c3a" : "#5D4037") + ";");
            cell.getChildren().add(lblJour);

            // Points colorés pour les propositions
            for (int i = 0; i < Math.min(props.size(), 2); i++) {
                Proposition p = props.get(i);
                Label dot = new Label("●");
                dot.setStyle("-fx-font-size:8;-fx-text-fill:" + couleurStatut(p.getStatut()) + ";");
                dot.setOnMouseClicked(e -> afficherTooltipProposition(p));
                cell.getChildren().add(dot);
            }
            if (props.size() > 2) {
                Label plus = new Label("+" + (props.size() - 2));
                plus.setStyle("-fx-font-size:9;-fx-text-fill:#9a9a9a;");
                cell.getChildren().add(plus);
            }

            gridCalendrier.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        // Jours du mois suivant (grisés)
        int jourSuivant = 1;
        while (col < 7) {
            Label lbl = new Label(String.valueOf(jourSuivant++));
            lbl.setStyle("-fx-font-size:12;-fx-text-fill:#c8b8a8;-fx-alignment:CENTER;-fx-padding:6 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            gridCalendrier.add(lbl, col, row);
            col++;
        }
    }

    private void afficherTooltipProposition(Proposition p) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Proposition");
        a.setHeaderText(p.getTitreAffichage());
        a.setContentText(
            "Produit : " + nvl(p.getNomProduit(), "—") + "\n" +
            "Statut : " + p.getStatut().toUpperCase() + "\n" +
            "Prix : " + String.format("%.2f TND", p.getPrixPropose()) + "\n\n" +
            p.getDescriptionCourte()
        );
        a.showAndWait();
    }

    private String couleurStatut(String statut) {
        if (statut == null) return "#9a9a9a";
        return switch (statut) {
            case "acceptee"    -> "#1e8e3e";
            case "refusee"     -> "#d93025";
            case "terminee"    -> "#7a5c3a";
            case "en_revision" -> "#1565c0";
            default            -> "#F5A623";
        };
    }

    private String nvl(String v, String d) { return (v == null || v.isBlank()) ? d : v; }

    // ── Actions FXML ─────────────────────────────────────────────────────────

    @FXML
    void handleAfficherFormulaire() {
        User u = SessionManager.getCurrentUser();
        if (u != null && u.getRole() == Role.ARTISANT) {
            showError("Accès refusé", "Les artisans ne peuvent que consulter les propositions.");
            return;
        }
        ouvrirFormulaire(null);
    }

    @FXML
    void handleRafraichir() { rafraichirListe(); }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            User u = SessionManager.getCurrentUser();
            if (u == null) { SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420); return; }
            if (u.getRole() == Role.ADMIN)
                SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art", 1200, 780);
            else
                SceneNavigator.navigate(event, "/fxml/MainView.fxml", "Artefact", 1200, 750);
        } catch (Exception e) { showError("Navigation", e.getMessage()); }
    }

    // ── Données ───────────────────────────────────────────────────────────────

    private void rafraichirListe() {
        try {
            User u = SessionManager.getCurrentUser();
            all.clear();
            if (u != null && u.getRole() == Role.CLIENT)
                all.addAll(service.afficherParUtilisateur(u.getId()));
            else
                all.addAll(service.afficher());

            // Remplir filtre produit
            cbFiltreProduit.getItems().clear();
            cbFiltreProduit.getItems().add("Tous les produits");
            all.stream().map(Proposition::getNomProduit)
               .filter(n -> n != null && !n.isBlank())
               .distinct()
               .forEach(n -> cbFiltreProduit.getItems().add(n));
            cbFiltreProduit.setValue("Tous les produits");

            appliquerFiltres();

            // Rafraîchir le calendrier si visible
            if (vboxCalendrier != null && vboxCalendrier.isVisible()) {
                rafraichirCalendrier();
            }
        } catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
    }

    private void appliquerFiltres() {
        propositionsContainer.getChildren().clear();

        String q      = tfRecherche.getText() == null ? "" : tfRecherche.getText().trim().toLowerCase(Locale.ROOT);
        String statut = cbFiltreStatut.getValue();
        String produit = cbFiltreProduit.getValue();
        User u = SessionManager.getCurrentUser();
        
        // ARTISAN : lecture seule uniquement
        boolean estArtisan = u != null && u.getRole() == Role.ARTISANT;
        boolean canEdit = !estArtisan && u != null && (u.getRole() == Role.ADMIN || u.getRole() == Role.ARTISANT);
        boolean canManage = canEdit;

        List<Proposition> filtered = all.stream()
                .filter(p -> q.isBlank() || matchRecherche(p, q))
                .filter(p -> statut == null || "Tous les statuts".equals(statut) || statut.equalsIgnoreCase(safe(p.getStatut())))
                .filter(p -> produit == null || "Tous les produits".equals(produit) || produit.equalsIgnoreCase(safe(p.getNomProduit())))
                .toList();

        // Grouper par matériau (nomProduit)
        Map<String, List<Proposition>> parMateriau = new LinkedHashMap<>();
        for (Proposition p : filtered) {
            String mat = safe(p.getNomProduit()).isBlank() ? "Autres" : p.getNomProduit();
            parMateriau.computeIfAbsent(mat, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<Proposition>> entry : parMateriau.entrySet()) {
            Label lblMat = new Label("Pour le matériau : " + entry.getKey());
            lblMat.setStyle("-fx-font-family: Georgia; -fx-font-size: 20; -fx-text-fill: #8D6E63; -fx-padding: 0 0 8 0;");
            propositionsContainer.getChildren().add(lblMat);

            FlowPane grille = new FlowPane();
            grille.setHgap(20); grille.setVgap(20);
            grille.setPrefWrapLength(980);

            for (Proposition prop : entry.getValue()) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PropositionCard.fxml"));
                    javafx.scene.Node card = loader.load();
                    PropositionCardController ctrl = loader.getController();
                    boolean myProp = u != null && prop.getUserId() == u.getId();
                    ctrl.setData(prop, canEdit || myProp, canManage,
                            this::ouvrirEdition,
                            this::confirmerSuppression,
                            this::changerStatut);
                    grille.getChildren().add(card);
                } catch (Exception e) { showError("Erreur UI", e.getMessage()); break; }
            }
            propositionsContainer.getChildren().add(grille);
        }

        boolean empty = filtered.isEmpty();
        lblAucuneProposition.setVisible(empty);
        lblAucuneProposition.setManaged(empty);
    }

    // ── Actions cartes ────────────────────────────────────────────────────────

    private void ouvrirFormulaire(Proposition p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterProposition.fxml"));
            javafx.scene.Parent root = loader.load();
            AjouterPropositionController ctrl = loader.getController();
            if (p != null) ctrl.setPropositionEnEdition(p);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(p == null ? "Nouvelle proposition" : "Modifier proposition #" + p.getId());
            stage.setScene(new javafx.scene.Scene(root, 780, 700));
            stage.setOnHidden(e -> rafraichirListe());
            stage.show();
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    private void ouvrirEdition(Proposition p) { 
        User u = SessionManager.getCurrentUser();
        if (u != null && u.getRole() == Role.ARTISANT) {
            showError("Accès refusé", "Les artisans ne peuvent pas modifier de propositions.");
            return;
        }
        ouvrirFormulaire(p); 
    }

    private void confirmerSuppression(Proposition p) {
        User u = SessionManager.getCurrentUser();
        if (u != null && u.getRole() == Role.ARTISANT) {
            showError("Accès refusé", "Les artisans ne peuvent pas supprimer de propositions.");
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Suppression"); a.setHeaderText("Supprimer cette proposition ?");
        a.setContentText(p.getTitreAffichage());
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { service.supprimer(p.getId()); rafraichirListe(); }
        catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
    }

    private void changerStatut(Proposition p, String statut) {
        try {
            User u = SessionManager.getCurrentUser();
            int artisanId = (u != null && u.getRole() == Role.ARTISANT) ? u.getId() : 0;
            service.changerStatut(p.getId(), statut, artisanId);
            rafraichirListe();

            String msgEmail = switch (statut) {
                case "acceptee" -> "Email de confirmation envoyé au client.";
                case "refusee"  -> "Email de refus envoyé au client.";
                case "terminee" -> "Email de fin de travail envoyé au client.";
                default         -> "Statut mis à jour.";
            };
            showInfo("Statut mis à jour",
                    "La proposition est maintenant : " + statut.toUpperCase() + "\n" + msgEmail);

        } catch (IllegalArgumentException e) {
            showError("Règle métier", e.getMessage());
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private boolean matchRecherche(Proposition p, String q) {
        return safe(p.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                || safe(p.getDescription()).toLowerCase(Locale.ROOT).contains(q)
                || safe(p.getNomProduit()).toLowerCase(Locale.ROOT).contains(q);
    }

    private String safe(String v) { return v == null ? "" : v; }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
