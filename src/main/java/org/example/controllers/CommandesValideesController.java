 package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.models.Commande;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Affiche uniquement les commandes avec statut "confirmee",
 * triées par date décroissante (back_liste_validee).
 */
public class CommandesValideesController {

    @FXML private Label lblAvatar;
    @FXML private Label lblNomAdmin;
    @FXML private VBox  valideesContainer;
    @FXML private Label lblAucune;
    @FXML private Label lblCount;

    private final ServiceCommande service = new ServiceCommande();
    private List<Commande> commandes;

    @FXML
    void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String nom = user.getNomComplet();
            lblNomAdmin.setText(nom);
            lblAvatar.setText(buildInitiales(nom));
        }
        chargerValidees();
    }

    private void chargerValidees() {
        valideesContainer.getChildren().clear();
        try {
            commandes = service.afficherValidees();
            int count = commandes.size();
            lblCount.setText(count + " commande(s)");
            lblAucune.setVisible(count == 0);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Commande c : commandes) {
                // Construire une ligne simple sans FXML dédié
                HBox row = new HBox(0);
                row.setStyle("-fx-background-color: white; -fx-padding: 10 16;"
                        + "-fx-border-color: transparent transparent #f0ece6 transparent;"
                        + "-fx-border-width: 0 0 1 0; -fx-alignment: CENTER_LEFT;");

                String dateStr = c.getDateCommande() != null
                        ? c.getDateCommande().toInstant().atZone(ZoneId.systemDefault())
                          .toLocalDateTime().format(fmt)
                        : "—";
                String nomClient = c.getNomClient() != null && !c.getNomClient().isBlank()
                        ? c.getNomClient().trim()
                        : (c.getClientId() != null ? "Client #" + c.getClientId() : "—");
                String totalStr  = c.getTotal() != null ? String.format("%.2f DT", c.getTotal()) : "—";
                String adresse   = c.getAdresseLivraison() != null ? c.getAdresseLivraison() : "—";

                row.getChildren().addAll(
                        lbl(safe(c.getNumero()), 200, true, "#4a1e2e"),
                        lbl(dateStr,             140, false, "#7a7570"),
                        lbl(nomClient,           180, false, "#1a1a1a"),
                        lbl(totalStr,            120, true,  "#c8763a"),
                        lbl(adresse,             -1,  false, "#7a7570")   // -1 = hgrow
                );

                valideesContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement : " + e.getMessage()).showAndWait();
        }
    }

    // ── Export PDF ──

    @FXML
    void handleExportPdf() {
        if (commandes == null || commandes.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune commande validée à exporter.").showAndWait();
            return;
        }
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("commandes_validees.pdf");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File fichier = chooser.showSaveDialog(valideesContainer.getScene().getWindow());
        if (fichier == null) return;

        try {
            genererPdf(fichier);
            new Alert(Alert.AlertType.INFORMATION, "PDF exporté :\n" + fichier.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur PDF : " + e.getMessage()).showAndWait();
        }
    }

    private void genererPdf(File fichier) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        try (PDDocument doc = new PDDocument()) {
            final float MARGIN = 40;
            final float PAGE_W = PDRectangle.A4.getWidth();
            final float PAGE_H = PDRectangle.A4.getHeight();
            final float ROW_H  = 16;
            final int   PER_PAGE = 30;

            int pages = (int) Math.ceil((double) commandes.size() / PER_PAGE);
            if (pages == 0) pages = 1;

            for (int p = 0; p < pages; p++) {
                PDPage pdPage = new PDPage(PDRectangle.A4);
                doc.addPage(pdPage);

                try (PDPageContentStream cs = new PDPageContentStream(doc, pdPage)) {
                    PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font reg  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                    float y = PAGE_H - MARGIN;

                    cs.setFont(bold, 15);
                    cs.beginText(); cs.newLineAtOffset(MARGIN, y);
                    cs.showText("afk'art – Commandes confirmées"); cs.endText();
                    y -= 18;

                    cs.setFont(reg, 9);
                    cs.beginText(); cs.newLineAtOffset(MARGIN, y);
                    cs.showText("Total : " + commandes.size() + "  |  Page " + (p+1) + "/" + pages);
                    cs.endText();
                    y -= 8;

                    cs.setLineWidth(0.5f);
                    cs.moveTo(MARGIN, y); cs.lineTo(PAGE_W - MARGIN, y); cs.stroke();
                    y -= 14;

                    // Colonnes
                    String[] cols   = {"N° Commande", "Date", "Client", "Total (DT)", "Adresse"};
                    float[]  widths = {150, 90, 110, 70, 135};

                    cs.setFont(bold, 9);
                    float x = MARGIN;
                    for (int i = 0; i < cols.length; i++) {
                        cs.beginText(); cs.newLineAtOffset(x, y);
                        cs.showText(cols[i]); cs.endText();
                        x += widths[i];
                    }
                    y -= 4;
                    cs.moveTo(MARGIN, y); cs.lineTo(PAGE_W - MARGIN, y); cs.stroke();
                    y -= ROW_H;

                    cs.setFont(reg, 8);
                    int start = p * PER_PAGE;
                    int end   = Math.min(start + PER_PAGE, commandes.size());
                    for (int i = start; i < end; i++) {
                        Commande c = commandes.get(i);
                        String dateStr  = c.getDateCommande() != null
                                ? c.getDateCommande().toInstant().atZone(ZoneId.systemDefault())
                                  .toLocalDateTime().format(fmt) : "—";
                        String client   = c.getNomClient() != null && !c.getNomClient().isBlank()
                                ? c.getNomClient().trim() : (c.getClientId() != null ? "#" + c.getClientId() : "—");
                        if (client.length() > 18) client = client.substring(0, 17) + "…";
                        String total    = c.getTotal() != null ? String.format("%.2f", c.getTotal()) : "—";
                        String adresse  = safe(c.getAdresseLivraison());
                        if (adresse.length() > 28) adresse = adresse.substring(0, 27) + "…";

                        String[] vals = { safe(c.getNumero()), dateStr, client, total, adresse };
                        x = MARGIN;
                        for (int j = 0; j < vals.length; j++) {
                            cs.beginText(); cs.newLineAtOffset(x, y);
                            cs.showText(vals[j]); cs.endText();
                            x += widths[j];
                        }
                        y -= ROW_H;
                    }
                }
            }
            doc.save(fichier);
        }
    }

    // ── Navigation ──

    @FXML void handleNavDashboard(ActionEvent e)    { nav(e, "/fxml/AdminDashboard.fxml",   "afk'art – Backoffice Admin"); }
    @FXML void handleNavArticles(ActionEvent e)     { nav(e, "/fxml/AfficherArticles.fxml", "Backoffice – Articles"); }
    @FXML void handleNavReclamations(ActionEvent e) { nav(e, "/fxml/ReclamationsView.fxml", "Backoffice – Réclamations"); }
    @FXML void handleNavCommandes(ActionEvent e)    { nav(e, "/fxml/GestionCommandes.fxml", "Backoffice – Commandes"); }

    @FXML void handleLogout(ActionEvent e) {
        try { SessionManager.logout();
              SceneNavigator.navigate(e, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    }

    private void nav(ActionEvent e, String fxml, String title) {
        try { SceneNavigator.navigate(e, fxml, title, 1200, 780); }
        catch (Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    }

    // ── Helpers ──

    private Label lbl(String text, double width, boolean bold, String color) {
        Label l = new Label(text);
        String style = "-fx-font-size: 12; -fx-text-fill: " + color + ";";
        if (bold) style += " -fx-font-weight: bold;";
        l.setStyle(style);
        l.setWrapText(false);
        if (width > 0) {
            l.setPrefWidth(width);
            l.setMinWidth(width);
            l.setMaxWidth(width);
        } else {
            javafx.scene.layout.HBox.setHgrow(l, javafx.scene.layout.Priority.ALWAYS);
        }
        return l;
    }

    private String safe(String s) { return s == null ? "—" : s; }

    private String buildInitiales(String fullName) {
        if (fullName == null || fullName.isBlank()) return "AD";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}

