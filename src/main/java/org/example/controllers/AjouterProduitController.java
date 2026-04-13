package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.example.models.Produit;
import org.example.services.ServiceProduit;

import java.io.File;

public class AjouterProduitController {
    @FXML
    private ComboBox<String> cbNom, cbTypeMateriau, cbEtat;
    @FXML
    private TextField tfQuantite, tfOrigine;
    @FXML
    private TextArea taDescription;
    @FXML
    private Slider sliderImpact;
    @FXML
    private Label lblImpactValue, lblImageName;

    private String selectedImageName = "default.jpg";

    @FXML
    public void initialize() {
        lblImpactValue.setText(String.valueOf((int) sliderImpact.getValue()));
        sliderImpact.valueProperty().addListener((obs, oldVal, newVal) ->
                lblImpactValue.setText(String.valueOf(newVal.intValue()))
        );
    }

    @FXML
    void handleUploadImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            // On conserve le nom de fichier pour la colonne SQL "image"
            selectedImageName = selectedFile.getName();
            lblImageName.setText(selectedImageName);
        }
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            ServiceProduit service = new ServiceProduit();
            String nom = cbNom.getValue();
            String typeMateriau = cbTypeMateriau.getValue();
            String etat = cbEtat.getValue();
            String origine = tfOrigine.getText() == null ? "" : tfOrigine.getText().trim();
            String description = taDescription.getText() == null ? "" : taDescription.getText().trim();

            if (nom == null || typeMateriau == null || etat == null || origine.isEmpty()) {
                throw new IllegalArgumentException("Veuillez remplir tous les champs obligatoires.");
            }

            int quantite;
            try {
                quantite = Integer.parseInt(tfQuantite.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La quantité doit être un entier valide.");
            }
            if (quantite <= 0 || quantite > 999999) {
                throw new IllegalArgumentException("La quantité doit être positive et <= 999 999.");
            }

            if (description.length() < 10) {
                throw new IllegalArgumentException("La description doit contenir au moins 10 caractères.");
            }

            int impact = (int) sliderImpact.getValue();
            if (impact < 0 || impact > 100) {
                throw new IllegalArgumentException("L'impact écologique doit être entre 0 et 100.");
            }

            Produit p = new Produit(
                    nom,
                    typeMateriau,
                    etat,
                    quantite,
                    origine,
                    description,
                    selectedImageName, // colonne SQL "image"
                    impact,
                    3, // artisan_id par défaut pour les tests
                    3  // user_id par défaut pour les tests
            );

            service.ajouter(p);
            new Alert(Alert.AlertType.INFORMATION, "Produit recyclable ajouté avec succès !").show();
            handleAnnuler();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnuler() {
        cbNom.setValue(null);
        cbTypeMateriau.setValue(null);
        cbEtat.setValue(null);
        tfQuantite.clear();
        tfOrigine.clear();
        taDescription.clear();
        sliderImpact.setValue(50);
        lblImpactValue.setText("50");
        selectedImageName = "default.jpg";
        lblImageName.setText("Aucune image sélectionnée");
    }
}

