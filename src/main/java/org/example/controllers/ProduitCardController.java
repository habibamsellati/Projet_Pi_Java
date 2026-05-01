package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import org.example.models.Produit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProduitCardController {

    @FXML private ImageView ivImage;
    @FXML private Label     lblNom;
    @FXML private Label     lblEtat;
    @FXML private Label     lblDescription;
    @FXML private Label     lblOrigine;
    @FXML private Label     lblQuantite;
    @FXML private Button    btnDetails;
    @FXML private Button    btnModifier;
    @FXML private Button    btnSupprimer;

    private Produit          produit;
    private Consumer<Produit> onModifier;
    private Consumer<Produit> onSupprimer;
    private boolean canEdit = true;

    public void setData(Produit produit, boolean canEdit, Consumer<Produit> onModifier, Consumer<Produit> onSupprimer) {
        this.produit     = produit;
        this.canEdit     = canEdit;
        this.onModifier  = onModifier;
        this.onSupprimer = onSupprimer;

        // Masquer les boutons si pas de droits d'édition
        btnModifier.setVisible(canEdit);
        btnModifier.setManaged(canEdit);
        btnSupprimer.setVisible(canEdit);
        btnSupprimer.setManaged(canEdit);

        // Nom
        lblNom.setText(produit.getNomAffichage());

        // Badge état avec couleur
        String etat = nvl(produit.getEtat(), "—");
        lblEtat.setText(etat.toUpperCase());
        lblEtat.setStyle(lblEtat.getStyle() + etatColor(etat));

        // Description courte
        lblDescription.setText(produit.getDescriptionCourte());

        // Localisation
        lblOrigine.setText(nvl(produit.getOrigine(), "Non renseignée"));

        // Quantité
        lblQuantite.setText(produit.getQuantite() + " Unité" + (produit.getQuantite() > 1 ? "s" : ""));

        // Image
        Image img = loadImage(produit.getImage());
        if (img != null) {
            ivImage.setImage(img);
        } else {
            // Fond coloré par défaut selon le nom du matériau
            ivImage.setStyle("-fx-background-color: " + defaultBg(produit.getNom()) + ";");
        }

        // Arrondi image en haut
        Rectangle clip = new Rectangle(300, 200);
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        ivImage.setClip(clip);
    }

    @FXML void handleModifier()  { if (onModifier  != null && produit != null) onModifier.accept(produit); }
    @FXML void handleSupprimer() { if (onSupprimer != null && produit != null) onSupprimer.accept(produit); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String etatColor(String etat) {
        return switch (etat.toLowerCase()) {
            case "excellent" -> "-fx-text-fill:#166534;-fx-background-color:white;";
            case "bon"       -> "-fx-text-fill:#1e4d2b;-fx-background-color:white;";
            case "moyen"     -> "-fx-text-fill:#92400e;-fx-background-color:white;";
            default          -> "-fx-text-fill:#7f1d1d;-fx-background-color:white;";
        };
    }

    private String defaultBg(String nom) {
        if (nom == null) return "#e8e0d8";
        return switch (nom.toLowerCase()) {
            case "plastique" -> "#d4edda";
            case "carton"    -> "#fff3cd";
            case "verre"     -> "#cce5ff";
            case "métal"     -> "#e2e3e5";
            case "bois"      -> "#f5e6d3";
            case "tissu"     -> "#f8d7da";
            default          -> "#f0ebe4";
        };
    }

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

    private String nvl(String v, String fallback) {
        return (v != null && !v.isBlank()) ? v : fallback;
    }
}
