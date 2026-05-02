package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import org.example.models.Article;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PanierItemController {
    @FXML private ImageView ivImage;
    @FXML private Label lblTitre;
    @FXML private Label lblAuteur;
    @FXML private Label lblDate;
    @FXML private Label lblPrix;

    private Runnable onRemove;
    private Runnable onVoir;

    public void setData(Article article) {
        lblTitre.setText(article.getTitre() != null ? article.getTitre() : "Sans titre");
        lblPrix.setText(article.getPrix() == null ? "Prix non renseigné" : String.format("%.2f DT", article.getPrix()));
        lblAuteur.setText("Artisan #" + article.getArtisanId());

        if (article.getDatePublication() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            lblDate.setText(article.getDatePublication().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(fmt));
        } else {
            lblDate.setText("Date inconnue");
        }

        if (ivImage != null) {
            Image img = loadImageSafely(article.getImageUrl());
            ivImage.setImage(img);
            Rectangle clip = new Rectangle(ivImage.getFitWidth(), ivImage.getFitHeight());
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            ivImage.setClip(clip);
        }
    }

    public void setOnRemove(Runnable onRemove) { this.onRemove = onRemove; }
    public void setOnVoir(Runnable onVoir)     { this.onVoir = onVoir; }

    @FXML void handleRemove() { if (onRemove != null) onRemove.run(); }
    @FXML void handleVoir()   { if (onVoir   != null) onVoir.run(); }

    private Image loadImageSafely(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        List<String> candidates = new ArrayList<>();
        String raw = imagePath.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:") || raw.startsWith("jar:")) {
            candidates.add(raw);
        }
        File localFile = new File(raw);
        if (localFile.exists()) candidates.add(localFile.toURI().toString());
        String cp = raw.startsWith("/") ? raw : "/" + raw;
        java.net.URL resource = getClass().getResource(cp);
        if (resource != null) candidates.add(resource.toExternalForm());
        for (String candidate : candidates) {
            try {
                Image image = new Image(candidate, false);
                if (!image.isError()) return image;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
