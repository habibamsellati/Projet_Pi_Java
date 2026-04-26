package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.example.models.Article;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClientArticleCardController {
    @FXML private ImageView ivImage;
    @FXML private Label lblDate;
    @FXML private Label lblTitre;
    @FXML private Label lblContenu;
    @FXML private Label lblPrix;
    @FXML private Button btnAjouterPanier;

    private Article currentArticle;
    private Runnable onAjouterPanier;

    public void setData(Article article) {
        this.currentArticle = article;
        lblTitre.setText(article.getTitre());
        lblContenu.setText(article.getContenu());
        lblPrix.setText(article.getPrix() == null ? "Prix non renseigné" : String.format("%.2f DT", article.getPrix()));

        if (article.getDatePublication() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            lblDate.setText(article.getDatePublication().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter));
        } else {
            lblDate.setText("Date inconnue");
        }

        ivImage.setImage(loadImageSafely(article.getImageUrl()));
        Rectangle clip = new Rectangle(ivImage.getFitWidth(), ivImage.getFitHeight());
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        ivImage.setClip(clip);
    }

    public Article getCurrentArticle() {
        return currentArticle;
    }

    public void setOnAjouterPanier(Runnable onAjouterPanier) {
        this.onAjouterPanier = onAjouterPanier;
    }

    public void setArticleDejaAuPanier(boolean dejaAuPanier) {
        if (btnAjouterPanier == null) {
            return;
        }
        btnAjouterPanier.setDisable(dejaAuPanier);
        btnAjouterPanier.setText(dejaAuPanier ? "Déjà au panier" : "Ajouter au panier");
    }

    @FXML
    void handleAjouterPanier() {
        if (onAjouterPanier != null) {
            onAjouterPanier.run();
        }
    }

    @FXML
    void handleLireSuite() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleCommentairesView.fxml"));
            Parent root = loader.load();

            ArticleCommentairesController controller = loader.getController();
            controller.setArticle(currentArticle);

            Stage stage = new Stage();
            stage.setTitle("Article et commentaires");
            stage.setScene(new Scene(root, 920, 700));
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la page commentaires : " + e.getMessage()).showAndWait();
        }
    }

    private Image loadImageSafely(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        String raw = imagePath.trim();

        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:") || raw.startsWith("jar:")) {
            candidates.add(raw);
        }

        File localFile = new File(raw);
        if (localFile.exists()) {
            candidates.add(localFile.toURI().toString());
        }

        String classpathPath = raw.startsWith("/") ? raw : "/" + raw;
        java.net.URL resource = getClass().getResource(classpathPath);
        if (resource != null) {
            candidates.add(resource.toExternalForm());
        }

        for (String candidate : candidates) {
            try {
                Image image = new Image(candidate, false);
                if (!image.isError()) {
                    return image;
                }
            } catch (Exception ignored) {
                // Ignore et essaie le candidat suivant.
            }
        }
        return null;
    }
}

