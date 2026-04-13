package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import org.example.models.Article;
import org.example.services.ServiceArticle;
import java.sql.SQLException;

public class ArticleCardController {

    @FXML private ImageView ivImage;
    @FXML private Label lblDate, lblTitre, lblAuteur, lblContenu, lblPrix;

    private Article currentArticle;
    private final ServiceArticle service = new ServiceArticle();

    /**
     * Remplit la carte avec les données de l'article
     */
    public void setData(Article a) {
        this.currentArticle = a;

        // Remplissage des textes
        lblTitre.setText(a.getTitre());
        lblContenu.setText(a.getContenu());
        lblPrix.setText(String.format("%.2f DT", a.getPrix()));

        // Simuler une date (ou utiliser a.getDate() si elle existe en BDD)
        lblDate.setText("23 AVR 2026");

        // --- GESTION DE L'IMAGE ---
        try {
            if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
                Image img = new Image(a.getImageUrl(), true); // 'true' pour chargement en arrière-plan
                ivImage.setImage(img);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image : " + e.getMessage());
            // Optionnel : ivImage.setImage(new Image("/images/default.png"));
        }

        // --- FIX : COINS ARRONDIS (Remplace le -fx-clip du CSS) ---
        // On crée un rectangle de la taille de l'ImageView avec des arcs arrondis
        Rectangle clip = new Rectangle(ivImage.getFitWidth(), ivImage.getFitHeight());
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        ivImage.setClip(clip);
    }

    @FXML
    void handleModifier(ActionEvent event) {
        // Logique pour ouvrir le formulaire de modification
        System.out.println("Ouverture modification pour : " + currentArticle.getTitre());

        // Tip: Tu peux charger AjouterArticle.fxml et remplir les champs avec currentArticle
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        // Création d'une alerte stylisée
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Voulez-vous vraiment supprimer cet article ?");
        alert.setContentText("Article : " + currentArticle.getTitre());

        // Personnalisation des boutons de l'alerte
        ButtonType btnOui = new ButtonType("Oui, Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnOui, btnNon);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                try {
                    service.supprimer(currentArticle.getId());

                    // Information de succès
                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Article supprimé avec succès !");
                    success.show();

                    // Note : Pour que la carte disparaisse de l'écran,
                    // il faudra appeler une méthode de rafraîchissement dans ton contrôleur principal.

                } catch (SQLException e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Erreur BDD : " + e.getMessage());
                    error.show();
                } catch (IllegalStateException e) {
                    new Alert(Alert.AlertType.WARNING, e.getMessage()).show();
                }
            }
        });
    }

    @FXML
    void handleLireSuite(ActionEvent event) {
        // Optionnel : Afficher les détails complets ou les commentaires
        System.out.println("Affichage des détails pour l'article " + currentArticle.getId());
    }
}