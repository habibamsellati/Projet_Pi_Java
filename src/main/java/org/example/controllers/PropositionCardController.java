package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import org.example.models.Proposition;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PropositionCardController {

    @FXML private ImageView ivImage;
    @FXML private Label     lblTitre;
    @FXML private Label     lblDescription;
    @FXML private Label     lblPrix;
    @FXML private Label     lblAvatar;
    @FXML private Label     lblUser;
    @FXML private Button    btnVoir;
    @FXML private Button    btnModifier;
    @FXML private Button    btnSupprimer;
    @FXML private HBox      boxActions;
    @FXML private HBox      boxStatut;
    @FXML private Button    btnAccepter;
    @FXML private Button    btnRefuser;
    @FXML private Button    btnTerminer;

    private Proposition                  proposition;
    private Consumer<Proposition>        onModifier;
    private Consumer<Proposition>        onSupprimer;
    private BiConsumer<Proposition, String> onChangerStatut;

    public void setData(Proposition proposition,
                        boolean canEdit,
                        boolean canManageStatus,
                        Consumer<Proposition> onModifier,
                        Consumer<Proposition> onSupprimer,
                        BiConsumer<Proposition, String> onChangerStatut) {
        this.proposition     = proposition;
        this.onModifier      = onModifier;
        this.onSupprimer     = onSupprimer;
        this.onChangerStatut = onChangerStatut;

        // Titre
        lblTitre.setText(proposition.getTitreAffichage());

        // Description courte
        lblDescription.setText(proposition.getDescriptionCourte());

        // Prix
        lblPrix.setText("🔥 Prix estimé : " + String.format("%.2f TND", proposition.getPrixPropose()));

        // Avatar initiale du produit
        String nomProduit = proposition.getNomProduit();
        String initiale = (nomProduit != null && !nomProduit.isBlank())
                ? String.valueOf(nomProduit.charAt(0)).toUpperCase() : "P";
        lblAvatar.setText(initiale);
        lblUser.setText(nomProduit != null ? nomProduit : "Produit inconnu");

        // Image
        Image img = loadImage(proposition.getImage());
        if (img != null) {
            ivImage.setImage(img);
        } else {
            ivImage.setStyle("-fx-background-color: #e8e0d8;");
        }
        Rectangle clip = new Rectangle(300, 200);
        clip.setArcWidth(32); clip.setArcHeight(32);
        ivImage.setClip(clip);

        // Boutons modifier/supprimer
        if (boxActions != null) {
            boxActions.setVisible(canEdit);
            boxActions.setManaged(canEdit);
        }

        // Boutons statut (Accepter/Refuser/Terminer)
        if (boxStatut != null) {
            boxStatut.setVisible(canManageStatus);
            boxStatut.setManaged(canManageStatus);
        }

        // Masquer les boutons selon le statut actuel
        String statut = proposition.getStatut();
        if (canManageStatus) {
            boolean estAcceptee = "acceptee".equals(statut);
            boolean estTerminee = "terminee".equals(statut) || "refusee".equals(statut);
            if (btnAccepter != null) { btnAccepter.setVisible(!estAcceptee && !estTerminee); btnAccepter.setManaged(!estAcceptee && !estTerminee); }
            if (btnRefuser  != null) { btnRefuser.setVisible(!estTerminee);  btnRefuser.setManaged(!estTerminee); }
            if (btnTerminer != null) { btnTerminer.setVisible(estAcceptee);  btnTerminer.setManaged(estAcceptee); }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML void handleVoir()      { if (onModifier  != null && proposition != null) onModifier.accept(proposition); }
    @FXML void handleModifier()  { if (onModifier  != null && proposition != null) onModifier.accept(proposition); }
    @FXML void handleSupprimer() { if (onSupprimer != null && proposition != null) onSupprimer.accept(proposition); }

    @FXML void handleAccepter() { changerStatut("acceptee"); }
    @FXML void handleRefuser()  { changerStatut("refusee"); }
    @FXML void handleTerminer() { changerStatut("terminee"); }

    private void changerStatut(String statut) {
        if (onChangerStatut != null && proposition != null)
            onChangerStatut.accept(proposition, statut);
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        List<String> candidates = new ArrayList<>();
        String raw = path.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:") || raw.startsWith("jar:"))
            candidates.add(raw);
        File f = new File(raw);
        if (f.exists()) candidates.add(f.toURI().toString());
        String cp = raw.startsWith("/") ? raw : "/" + raw;
        java.net.URL res = getClass().getResource(cp);
        if (res != null) candidates.add(res.toExternalForm());
        for (String c : candidates) {
            try { Image img = new Image(c, false); if (!img.isError()) return img; }
            catch (Exception ignored) {}
        }
        return null;
    }
}
