package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceArticle;
import org.example.utils.SessionManager;
import java.io.File;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ArticleCardController {

    @FXML private ImageView ivImage;
    @FXML private Label lblDate, lblTitre, lblAuteur, lblContenu, lblPrix;
    @FXML private Button btnModifier;

    private Article currentArticle;
    private final ServiceArticle service = new ServiceArticle();
    private Runnable onRefresh;

    /**
     * Remplit la carte avec les données de l'article
     */
    public void setData(Article a) {
        this.currentArticle = a;

        // Remplissage des textes
        lblTitre.setText(a.getTitre());
        lblContenu.setText(a.getContenu());
        lblPrix.setText(a.getPrix() == null ? "Non renseigne" : String.format("%.2f DT", a.getPrix()));
        lblAuteur.setText("Artisan #" + a.getArtisanId());

        if (a.getDatePublication() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            lblDate.setText(a.getDatePublication().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter));
        } else {
            lblDate.setText("Date inconnue");
        }

        Image img = loadImageSafely(a.getImageUrl());
        ivImage.setImage(img);

        // --- FIX : COINS ARRONDIS (Remplace le -fx-clip du CSS) ---
        // On crée un rectangle de la taille de l'ImageView avec des arcs arrondis
        Rectangle clip = new Rectangle(ivImage.getFitWidth(), ivImage.getFitHeight());
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        ivImage.setClip(clip);

        // L'admin ne peut pas modifier un article depuis le dashboard.
        User user = SessionManager.getCurrentUser();
        boolean isAdmin = user != null && user.getRole() == Role.ADMIN;
        if (btnModifier != null) {
            btnModifier.setVisible(!isAdmin);
            btnModifier.setManaged(!isAdmin);
        }
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
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

        if (!raw.startsWith("/")) {
            java.net.URL imageFolderResource = getClass().getResource("/images/" + raw);
            if (imageFolderResource != null) {
                candidates.add(imageFolderResource.toExternalForm());
            }
        }

        for (String candidate : candidates) {
            try {
                Image image = new Image(candidate, false);
                if (!image.isError()) {
                    return image;
                }
            } catch (Exception ignored) {
                // Ignore et teste le candidat suivant.
            }
        }

        return null;
    }

    @FXML
    void handleModifier(ActionEvent event) {
        if (currentArticle == null) return;

        if (!canModifyArticle()) {
            new Alert(Alert.AlertType.WARNING,
                    "Accès refusé : l'admin ne peut pas modifier les articles depuis ce dashboard.")
                    .showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ModifierArticle.fxml"));
            Parent root = loader.load();

            ModifierArticleController controller = loader.getController();
            controller.setArticle(currentArticle);
            controller.setOnSaved(onRefresh);

            Stage owner = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier l'article — " + currentArticle.getTitre());
            stage.setScene(new Scene(root, 520, 640));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossible d'ouvrir la fenêtre de modification :\n" + e.getMessage())
                    .showAndWait();
        }
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (!canDeleteArticle()) {
            new Alert(Alert.AlertType.WARNING,
                    "Accès refusé : vous ne pouvez supprimer que vos propres articles.")
                    .showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Voulez-vous vraiment supprimer cet article ?");
        alert.setContentText("Article : " + currentArticle.getTitre());

        ButtonType btnOui = new ButtonType("Oui, Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnOui, btnNon);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                try {
                    service.supprimer(currentArticle.getId());
                    if (onRefresh != null) onRefresh.run();
                    new Alert(Alert.AlertType.INFORMATION, "Article supprimé avec succès !").showAndWait();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR,
                            "Erreur lors de la suppression :\n" + e.getMessage()).showAndWait();
                }
            }
        });
    }

    @FXML
    void handleLireSuite(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleCommentairesView.fxml"));
            Parent root = loader.load();

            ArticleCommentairesController controller = loader.getController();
            controller.setArticle(currentArticle);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Article et commentaires");
            stage.setScene(new Scene(root, 920, 700));
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les commentaires : " + e.getMessage()).showAndWait();
        }
    }

    private boolean canDeleteArticle() {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getRole() == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getRole() == Role.ARTISANT && currentArticle != null && currentArticle.getArtisanId() == user.getId();
    }

    private boolean canModifyArticle() {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == Role.ARTISANT && currentArticle != null && currentArticle.getArtisanId() == user.getId();
    }
}