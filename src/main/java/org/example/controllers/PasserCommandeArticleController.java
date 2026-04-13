package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.models.Commande;
import org.example.services.ServiceCommandeArticle;

import java.io.File;

public class PasserCommandeArticleController {
    @FXML
    private TextField tfArticleId, tfQuantite, tfAdresseLivraison, tfTelephone;
    @FXML
    private Label lblTitre, lblImage, lblPrixUnitaire, lblTotal;
    @FXML
    private ImageView imageViewArticle;

    private ServiceCommandeArticle.ArticleApercu current;

    @FXML
    void handleChargerArticle(ActionEvent event) {
        try {
            int articleId = Integer.parseInt(tfArticleId.getText().trim());
            ServiceCommandeArticle service = new ServiceCommandeArticle();
            current = service.getArticleApercu(articleId);
            if (current == null) {
                throw new IllegalArgumentException("Aucun article trouvé pour cet ID.");
            }

            lblTitre.setText(current.getTitre());
            lblImage.setText(current.getImage() == null ? "-" : current.getImage());
            lblPrixUnitaire.setText(String.format("%.2f DT", current.getPrix()));
            chargerImage(current.getImage());
            recalculerTotal();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleRecalculer(ActionEvent event) {
        try {
            recalculerTotal();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleValiderCommande(ActionEvent event) {
        try {
            if (current == null) {
                throw new IllegalArgumentException("Veuillez charger un article avant de valider.");
            }

            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            String adresse = tfAdresseLivraison.getText() == null ? "" : tfAdresseLivraison.getText().trim();
            String tel = tfTelephone.getText() == null ? "" : tfTelephone.getText().trim();

            ServiceCommandeArticle service = new ServiceCommandeArticle();
            Commande commande = service.creerCommandeArticle(
                    3,
                    current.getId(),
                    quantite,
                    adresse,
                    tel
            );

            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Commande créée avec succès !\nID commande: " + commande.getId() +
                            "\nMontant total à payer: " + String.format("%.2f DT", commande.getTotal())
            ).show();

            handleAnnuler();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnuler() {
        tfArticleId.clear();
        tfQuantite.setText("1");
        tfAdresseLivraison.clear();
        tfTelephone.clear();
        lblTitre.setText("-");
        lblImage.setText("-");
        lblPrixUnitaire.setText("0.00 DT");
        lblTotal.setText("0.00 DT");
        imageViewArticle.setImage(null);
        current = null;
    }

    private void recalculerTotal() {
        int quantite;
        try {
            quantite = Integer.parseInt(tfQuantite.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La quantité doit être un entier valide.");
        }
        if (quantite <= 0) throw new IllegalArgumentException("La quantité doit être positive.");

        double prix = (current == null) ? 0.0 : current.getPrix();
        lblTotal.setText(String.format("%.2f DT", quantite * prix));
    }

    private void chargerImage(String imageName) {
        imageViewArticle.setImage(null);
        if (imageName == null || imageName.isBlank()) return;
        try {
            File file = new File(imageName);
            if (file.exists()) {
                imageViewArticle.setImage(new Image(file.toURI().toString(), true));
                return;
            }
            imageViewArticle.setImage(new Image(imageName, true));
        } catch (Exception ignored) {
        }
    }
}

