package org.example.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.models.Commande;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur backoffice commandes — accès exclusif ADMIN.
 * Fonctionnalités : liste filtrée, valider/invalider, export PDF, statistiques.
 */
public class GestionCommandesController {

    // ── Stats ──
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblConfirmees;
    @FXML private Label lblEnAttente;
    @FXML private Label lblChiffreAffaires;

    // ── Filtres ──
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbStatut;

    // ── Liste ──
    @FXML private VBox commandesContainer;
    @FXML private Label lblAucune;

    private final ServiceCommande service = new ServiceCommande();
    private List<Commande> commandesCourantes;

    // ══════════════════════════════════════════
    //  Initialisation
    // ══════════════════════════════════════════

    @FXML
    void initialize() {
        checkAdminAccess();

        // Remplir le ComboBox statut
        cbStatut.getItems().setAll("Tous", "en_attente", "confirmee", "livree", "annulee");
        cbStatut.getSelectionModel().selectFirst();

        chargerStats();
        chargerCommandes(null, null);

        tfRecherche.textProperty().addListener((obs, ov, nv) -> chargerCommandes(nv, cbStatut.getValue()));
        cbStatut.valueProperty().addListener((obs, ov, nv) -> chargerCommandes(tfRecherche.getText(), nv));
    }

    private void checkAdminAccess() {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getRole() != Role.ADMIN) {
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.ERROR,
                        "Accès refusé : le module Commandes est réservé à l'administrateur principal.")
                        .showAndWait();
            });
        }
    }

    // ══════════════════════════════════════════
    //  Chargement des stats
    // ══════════════════════════════════════════

    private void chargerStats() {
        try {
            int total = service.countTotal();
            lblTotalCommandes.setText(String.valueOf(total));

            int confirmees = 0, enAttente = 0;
            for (String[] stat : service.getStatsByStatut()) {
                switch (stat[0]) {
                    case "confirmee" -> confirmees = Integer.parseInt(stat[1]);
                    case "en_attente" -> enAttente = Integer.parseInt(stat[1]);
                }
            }
            lblConfirmees.setText(String.valueOf(confirmees));
            lblEnAttente.setText(String.valueOf(enAttente));

            double ca = service.getTotalRevenue();
            lblChiffreAffaires.setText(String.format("%.2f DT", ca));
        } catch (Exception e) {
            System.err.println("Erreur stats commandes : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    //  Chargement de la liste
    // ══════════════════════════════════════════

    private void chargerCommandes(String motCle, String statut) {
        commandesContainer.getChildren().clear();
        try {
            commandesCourantes = service.afficherAvecFiltres(motCle, statut);
            boolean vide = commandesCourantes.isEmpty();
            lblAucune.setVisible(vide);

            for (Commande c : commandesCourantes) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CommandeRow.fxml"));
                Node row = loader.load();
                CommandeRowController ctrl = loader.getController();
                ctrl.setData(c);
                ctrl.setOnRefresh(() -> {
                    chargerStats();
                    chargerCommandes(tfRecherche.getText(), cbStatut.getValue());
                });
                commandesContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement commandes : " + e.getMessage()).showAndWait();
        }
    }

    // ══════════════════════════════════════════
    //  Actions boutons filtres
    // ══════════════════════════════════════════

    @FXML
    void handleFiltrer() {
        chargerCommandes(tfRecherche.getText(), cbStatut.getValue());
    }

    @FXML
    void handleReinitialiser() {
        tfRecherche.clear();
        cbStatut.getSelectionModel().selectFirst();
        chargerCommandes(null, null);
    }

    // ══════════════════════════════════════════
    //  Export PDF
    // ══════════════════════════════════════════

    @FXML
    void handleExportPdf() {
        if (commandesCourantes == null || commandesCourantes.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune commande à exporter.").showAndWait();
            return;
        }

        // Sélecteur de fichier
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("commandes.pdf");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File fichier = chooser.showSaveDialog(commandesContainer.getScene().getWindow());
        if (fichier == null) return;

        try {
            genererPdf(fichier, commandesCourantes);
            new Alert(Alert.AlertType.INFORMATION, "PDF exporté avec succès :\n" + fichier.getAbsolutePath())
                    .showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur génération PDF : " + e.getMessage()).showAndWait();
        }
    }

    private void genererPdf(File fichier, List<Commande> commandes) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (PDDocument doc = new PDDocument()) {
            // ── Page 1 (et suivantes si besoin) ──
            final float MARGIN = 40;
            final float PAGE_W = PDRectangle.A4.getWidth();
            final float PAGE_H = PDRectangle.A4.getHeight();
            final float COL_H  = 16;
            final int   ROWS_PER_PAGE = 30;

            int totalPages = (int) Math.ceil((double) commandes.size() / ROWS_PER_PAGE);
            if (totalPages == 0) totalPages = 1;

            for (int page = 0; page < totalPages; page++) {
                PDPage pdPage = new PDPage(PDRectangle.A4);
                doc.addPage(pdPage);

                try (PDPageContentStream cs = new PDPageContentStream(doc, pdPage)) {
                    PDType1Font fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                    float y = PAGE_H - MARGIN;

                    // ── En-tête ──
                    cs.setFont(fontBold, 16);
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText("afk'art – Liste des commandes");
                    cs.endText();
                    y -= 20;

                    cs.setFont(fontRegular, 10);
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y);
                    String statutFiltre = cbStatut.getValue() != null ? cbStatut.getValue() : "Tous";
                    cs.showText("Filtre statut : " + statutFiltre
                            + "   |   Total : " + commandes.size() + " commandes"
                            + "   |   Page " + (page + 1) + "/" + totalPages);
                    cs.endText();
                    y -= 6;

                    // ligne séparatrice
                    cs.setLineWidth(0.5f);
                    cs.moveTo(MARGIN, y);
                    cs.lineTo(PAGE_W - MARGIN, y);
                    cs.stroke();
                    y -= 14;

                    // ── En-têtes colonnes ──
                    String[] cols = {"N° Commande", "Date", "Client", "Statut", "Total (DT)", "Adresse"};
                    float[] widths = {130, 80, 100, 65, 60, 145};
                    cs.setFont(fontBold, 9);
                    float x = MARGIN;
                    for (int i = 0; i < cols.length; i++) {
                        cs.beginText();
                        cs.newLineAtOffset(x, y);
                        cs.showText(cols[i]);
                        cs.endText();
                        x += widths[i];
                    }
                    y -= 4;
                    cs.moveTo(MARGIN, y);
                    cs.lineTo(PAGE_W - MARGIN, y);
                    cs.stroke();
                    y -= COL_H;

                    // ── Lignes ──
                    cs.setFont(fontRegular, 8);
                    int start = page * ROWS_PER_PAGE;
                    int end   = Math.min(start + ROWS_PER_PAGE, commandes.size());

                    for (int i = start; i < end; i++) {
                        Commande c = commandes.get(i);
                        String dateStr = c.getDateCommande() != null
                                ? c.getDateCommande().toInstant().atZone(ZoneId.systemDefault())
                                  .toLocalDateTime().format(fmt)
                                : "—";
                        String clientStr = c.getNomClient() != null && !c.getNomClient().isBlank()
                                ? c.getNomClient() : (c.getClientId() != null ? "#" + c.getClientId() : "—");
                        String adresse = c.getAdresseLivraison() != null ? c.getAdresseLivraison() : "—";
                        if (adresse.length() > 30) adresse = adresse.substring(0, 28) + "…";

                        String[] vals = {
                                safe(c.getNumero()),
                                dateStr,
                                clientStr.length() > 18 ? clientStr.substring(0, 17) + "…" : clientStr,
                                safe(c.getStatut()),
                                c.getTotal() != null ? String.format("%.2f", c.getTotal()) : "—",
                                adresse
                        };

                        x = MARGIN;
                        for (int j = 0; j < vals.length; j++) {
                            cs.beginText();
                            cs.newLineAtOffset(x, y);
                            cs.showText(vals[j]);
                            cs.endText();
                            x += widths[j];
                        }
                        y -= COL_H;
                    }
                }
            }

            doc.save(fichier);
        }
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }

    // ══════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════

    @FXML void handleNavDashboard(ActionEvent e) {
        navigate(e, "/fxml/AdminDashboard.fxml", "afk'art – Backoffice Admin");
    }

    @FXML void handleNavArticles(ActionEvent e) {
        navigate(e, "/fxml/AfficherArticles.fxml", "Backoffice – Articles");
    }

    @FXML void handleNavReclamations(ActionEvent e) {
        navigate(e, "/fxml/ReclamationsView.fxml", "Backoffice – Réclamations");
    }

    @FXML void handleNavValidees(ActionEvent e) {
        navigate(e, "/fxml/CommandesValidees.fxml", "Backoffice – Commandes validées");
    }

    @FXML void handleLogout(ActionEvent e) {
        try {
            SessionManager.logout();
            SceneNavigator.navigate(e, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void navigate(ActionEvent e, String fxml, String title) {
        try {
            SceneNavigator.navigate(e, fxml, title, 1200, 780);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private String buildInitiales(String fullName) {
        if (fullName == null || fullName.isBlank()) return "AD";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}

