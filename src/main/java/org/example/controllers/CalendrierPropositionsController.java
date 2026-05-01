package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calendrier des propositions — vue mensuelle JavaFX.
 * Artisan : voit ses propres propositions.
 * Admin   : voit toutes les propositions.
 */
public class CalendrierPropositionsController {

    @FXML private Label     lblMoisAnnee;
    @FXML private Label     lblSousTitre;
    @FXML private GridPane  gridJours;
    @FXML private GridPane  gridCalendrier;
    @FXML private VBox      vboxDetail;
    @FXML private Label     lblDetailTitre;
    @FXML private Label     lblDetailProduit;
    @FXML private Label     lblDetailStatut;
    @FXML private Label     lblDetailPrix;
    @FXML private Label     lblDetailDate;

    private final ServiceProposition service = new ServiceProposition();
    private YearMonth moisCourant = YearMonth.now();
    private List<Proposition> propositions = new ArrayList<>();

    private static final String[] JOURS_SEMAINE = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    @FXML
    public void initialize() {
        User u = SessionManager.getCurrentUser();
        if (u != null) {
            lblSousTitre.setText(u.getRole() == Role.ADMIN
                    ? "Vue globale — toutes les propositions"
                    : "Votre planning — " + u.getNomComplet());
        }
        chargerEtAfficher();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML void handleMoisPrecedent() { moisCourant = moisCourant.minusMonths(1); chargerEtAfficher(); }
    @FXML void handleMoisSuivant()   { moisCourant = moisCourant.plusMonths(1);  chargerEtAfficher(); }
    @FXML void handleAujourdhui()    { moisCourant = YearMonth.now();            chargerEtAfficher(); }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            User u = SessionManager.getCurrentUser();
            if (u == null) return;
            if (u.getRole() == Role.ADMIN)
                SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art", 1200, 780);
            else
                SceneNavigator.navigate(event, "/fxml/MainView.fxml", "Artefact", 1200, 750);
        } catch (Exception e) { System.err.println("[Calendrier] " + e.getMessage()); }
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerEtAfficher() {
        // Titre mois
        String nomMois = moisCourant.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        nomMois = Character.toUpperCase(nomMois.charAt(0)) + nomMois.substring(1);
        lblMoisAnnee.setText(nomMois + " " + moisCourant.getYear());

        // Charger propositions
        try {
            User u = SessionManager.getCurrentUser();
            if (u != null && u.getRole() == Role.ARTISANT)
                propositions = service.afficherParUtilisateur(u.getId());
            else
                propositions = service.afficher();
        } catch (SQLException e) {
            System.err.println("[Calendrier] Erreur chargement: " + e.getMessage());
            propositions = new ArrayList<>();
        }

        // Indexer par date
        Map<LocalDate, List<Proposition>> parDate = new HashMap<>();
        for (Proposition p : propositions) {
            if (p.getDateCreation() != null) {
                LocalDate date = p.getDateCreation().toLocalDateTime().toLocalDate();
                parDate.computeIfAbsent(date, k -> new ArrayList<>()).add(p);
            }
        }

        afficherCalendrier(parDate);
        vboxDetail.setVisible(false);
        vboxDetail.setManaged(false);
    }

    // ── Affichage grille ──────────────────────────────────────────────────────

    private void afficherCalendrier(Map<LocalDate, List<Proposition>> parDate) {
        gridJours.getChildren().clear();
        gridCalendrier.getChildren().clear();

        // Configurer colonnes (7 jours)
        for (int i = 0; i < 7; i++) {
            gridJours.getColumnConstraints().clear();
            gridCalendrier.getColumnConstraints().clear();
        }
        for (int i = 0; i < 7; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            gridJours.getColumnConstraints().add(cc);
            gridCalendrier.getColumnConstraints().add(new javafx.scene.layout.ColumnConstraints(cc.getPrefWidth(), cc.getPrefWidth(), Double.MAX_VALUE, Priority.ALWAYS, null, true));
        }

        // En-têtes jours
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(JOURS_SEMAINE[i]);
            lbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #8a7060;" +
                         "-fx-alignment: CENTER; -fx-padding: 6 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            gridJours.add(lbl, i, 0);
        }

        // Premier jour du mois (1=Lundi, 7=Dimanche en ISO)
        LocalDate premierJour = moisCourant.atDay(1);
        int debutColonne = premierJour.getDayOfWeek().getValue() - 1; // 0-6
        int nbJours = moisCourant.lengthOfMonth();
        LocalDate aujourdhui = LocalDate.now();

        int col = debutColonne;
        int row = 0;

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            List<Proposition> props = parDate.getOrDefault(date, new ArrayList<>());

            VBox cellule = buildCellule(jour, date, props, aujourdhui);
            gridCalendrier.add(cellule, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private VBox buildCellule(int jour, LocalDate date, List<Proposition> props, LocalDate aujourdhui) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(6));
        cell.setMinHeight(80);
        cell.setMaxWidth(Double.MAX_VALUE);

        boolean estAujourdhui = date.equals(aujourdhui);
        boolean estWeekend = date.getDayOfWeek().getValue() >= 6;

        String bgColor = estAujourdhui ? "#fff3e0" : estWeekend ? "#faf8f5" : "white";
        cell.setStyle("-fx-background-color: " + bgColor + ";" +
                      "-fx-border-color: #e8e0d8; -fx-border-width: 0.5;");

        // Numéro du jour
        Label lblJour = new Label(String.valueOf(jour));
        lblJour.setStyle("-fx-font-size: " + (estAujourdhui ? "14" : "12") + ";" +
                         "-fx-font-weight: " + (estAujourdhui ? "bold" : "normal") + ";" +
                         "-fx-text-fill: " + (estAujourdhui ? "#7a5c3a" : "#5D4037") + ";");
        cell.getChildren().add(lblJour);

        // Propositions du jour (max 3 affichées)
        int affichees = 0;
        for (Proposition p : props) {
            if (affichees >= 3) {
                Label plus = new Label("+" + (props.size() - 3) + " autres");
                plus.setStyle("-fx-font-size: 10; -fx-text-fill: #9a9a9a;");
                cell.getChildren().add(plus);
                break;
            }
            Label evt = new Label(truncate(p.getTitreAffichage(), 14));
            evt.setMaxWidth(Double.MAX_VALUE);
            evt.setStyle("-fx-background-color: " + couleurStatut(p.getStatut()) + ";" +
                         "-fx-text-fill: white; -fx-background-radius: 4;" +
                         "-fx-padding: 2 5; -fx-font-size: 10; -fx-cursor: hand;");
            evt.setOnMouseClicked(e -> afficherDetail(p));
            cell.getChildren().add(evt);
            affichees++;
        }

        return cell;
    }

    private void afficherDetail(Proposition p) {
        lblDetailTitre.setText(p.getTitreAffichage());
        lblDetailProduit.setText("Produit : " + nvl(p.getNomProduit(), "—"));
        lblDetailStatut.setText("Statut : " + p.getStatut().toUpperCase());
        lblDetailStatut.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + couleurStatut(p.getStatut()) + ";");
        lblDetailPrix.setText("Prix proposé : " + String.format("%.2f TND", p.getPrixPropose()));
        lblDetailDate.setText("Date : " + (p.getDateCreation() != null
                ? p.getDateCreation().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—"));
        vboxDetail.setVisible(true);
        vboxDetail.setManaged(true);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String couleurStatut(String statut) {
        if (statut == null) return "#9a9a9a";
        return switch (statut) {
            case "acceptee"   -> "#1e8e3e";
            case "refusee"    -> "#d93025";
            case "terminee"   -> "#7a5c3a";
            case "en_revision"-> "#1565c0";
            default           -> "#F5A623";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private String nvl(String v, String def) { return (v == null || v.isBlank()) ? def : v; }
}
