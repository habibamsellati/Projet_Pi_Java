package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.example.models.Produit;
import org.example.models.Proposition;
import org.example.services.ServiceProduit;
import org.example.services.ServiceProposition;

import java.io.File;
import java.sql.SQLException;

public class AjouterPropositionController {
    @FXML
    private ComboBox<Produit> cbProduits;
    @FXML
    private TextField tfTitre, tfPrixPropose, tfTelephone;
    @FXML
    private TextArea taDescription;
    @FXML
    private Label lblProduitInfo, lblProduitImageName, lblDiffPourcentage;
    @FXML
    private ImageView imageViewProduit;

    private String selectedImageName = "default.jpg";

    @FXML
    public void initialize() {
        cbProduits.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produit p) {
                if (p == null) return "";
                return p.getNom() + " — " + p.getTypeMateriau() + " (" + p.getEtat() + ")";
            }

            @Override
            public Produit fromString(String s) {
                return null;
            }
        });

        lblProduitInfo.setText(
                "Cliquez sur « Actualiser la liste » pour charger les produits depuis MySQL (serveur démarré, base piprojet1)."
        );

        cbProduits.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, p) -> {
            if (p == null) {
                lblProduitInfo.setText(
                        cbProduits.getItems().isEmpty()
                                ? "Cliquez sur « Actualiser la liste » pour charger les produits."
                                : "Sélectionnez un produit dans la liste."
                );
                lblProduitImageName.setText("-");
                imageViewProduit.setImage(null);
                lblDiffPourcentage.setText("-");
                return;
            }
            afficherDetailsProduit(p);
        });
    }

    @FXML
    void handleActualiserProduits(ActionEvent event) {
        try {
            chargerListeProduits();
            new Alert(
                    Alert.AlertType.INFORMATION,
                    cbProduits.getItems().isEmpty()
                            ? "Aucun produit en base."
                            : cbProduits.getItems().size() + " produit(s) chargé(s)."
            ).show();
        } catch (Exception e) {
            lblProduitInfo.setText("Connexion impossible : " + e.getMessage());
            cbProduits.setItems(FXCollections.observableArrayList());
            new Alert(
                    Alert.AlertType.ERROR,
                    "Impossible de joindre MySQL. Démarrez le serveur (localhost) et vérifiez le port dans MyDatabase.\n\n"
                            + e.getMessage()
            ).show();
        }
    }

    private void chargerListeProduits() throws SQLException {
        ServiceProduit sp = new ServiceProduit();
        cbProduits.setItems(FXCollections.observableArrayList(sp.afficher()));
        if (cbProduits.getItems().isEmpty()) {
            lblProduitInfo.setText("Aucun produit dans la base. Ajoutez-en un puis actualisez.");
        } else {
            lblProduitInfo.setText("Sélectionnez un produit dans la liste.");
        }
    }

    private void afficherDetailsProduit(Produit p) {
        lblProduitInfo.setText(
                p.getNom() +
                        " | " + p.getTypeMateriau() +
                        " | État: " + p.getEtat() +
                        " | Stock: " + p.getQuantite() +
                        " | Origine: " + p.getOrigine() +
                        " | Impact: " + p.getImpactEcologique() + "/100"
        );
        lblProduitImageName.setText(p.getImage() == null ? "-" : p.getImage());
        chargerImage(p.getImage());
        lblDiffPourcentage.setText("-");
    }

    @FXML
    void handleUploadImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            selectedImageName = selectedFile.getName();
        }
    }

    @FXML
    void handleCalculerDiff(ActionEvent event) {
        try {
            Produit p = cbProduits.getSelectionModel().getSelectedItem();
            if (p == null) {
                throw new IllegalArgumentException("Sélectionnez d'abord un produit.");
            }
            double prixPropose = Double.parseDouble(tfPrixPropose.getText().trim());
            if (prixPropose <= 0) {
                throw new IllegalArgumentException("Le prix proposé doit être un nombre positif.");
            }

            ServiceProposition service = new ServiceProposition();
            double diff = service.calculerDifferencePourcentage(p.getId(), prixPropose);
            lblDiffPourcentage.setText(String.format("%+.2f%%", diff));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleEnvoyer(ActionEvent event) {
        try {
            Produit p = cbProduits.getSelectionModel().getSelectedItem();
            if (p == null) {
                throw new IllegalArgumentException("Sélectionnez un produit.");
            }
            int produitId = p.getId();

            String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
            String description = taDescription.getText() == null ? "" : taDescription.getText().trim();
            String telephone = tfTelephone.getText() == null ? "" : tfTelephone.getText().trim();

            double prixPropose;
            try {
                prixPropose = Double.parseDouble(tfPrixPropose.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Prix proposé invalide.");
            }
            if (prixPropose <= 0) {
                throw new IllegalArgumentException("Le prix proposé doit être un nombre positif.");
            }

            if (!telephone.matches("^[0-9]{8}$")) {
                throw new IllegalArgumentException("Le téléphone doit contenir exactement 8 chiffres (format تونس).");
            }

            if (titre.isEmpty() || description.isEmpty()) {
                throw new IllegalArgumentException("Veuillez remplir le titre et la description.");
            }

            Proposition prop = new Proposition(
                    produitId,
                    3,
                    titre,
                    description,
                    prixPropose,
                    telephone,
                    selectedImageName,
                    ServiceProposition.STATUT_EN_ATTENTE
            );

            ServiceProposition service = new ServiceProposition();
            service.ajouter(prop);
            new Alert(Alert.AlertType.INFORMATION, "Offre envoyée avec succès !").show();
            handleAnnuler();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnuler() {
        cbProduits.getSelectionModel().clearSelection();
        tfTitre.clear();
        tfPrixPropose.clear();
        tfTelephone.clear();
        taDescription.clear();
        lblProduitInfo.setText(
                "Cliquez sur « Actualiser la liste » pour charger les produits depuis MySQL (serveur démarré, base piprojet1)."
        );
        lblProduitImageName.setText("-");
        lblDiffPourcentage.setText("-");
        imageViewProduit.setImage(null);
        selectedImageName = "default.jpg";
    }

    private void chargerImage(String imageName) {
        imageViewProduit.setImage(null);
        if (imageName == null || imageName.isBlank()) return;
        try {
            File file = new File(imageName);
            if (file.exists()) {
                imageViewProduit.setImage(new Image(file.toURI().toString(), true));
                return;
            }
            imageViewProduit.setImage(new Image(imageName, true));
        } catch (Exception ignored) {
        }
    }
}
