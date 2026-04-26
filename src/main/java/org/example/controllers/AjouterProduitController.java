package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.models.Produit;
import org.example.services.ServiceProduit;

import java.sql.SQLException;

public class AjouterProduitController {

    @FXML private ComboBox<String> cbNom, cbType, cbEtat;
    @FXML private TextField tfQuantite, tfOrigine;
    @FXML private TextArea taDescription;
    @FXML private Slider sldImpact;
    @FXML private Label lblImpactValue;

    private final ServiceProduit sp = new ServiceProduit();
    private String imagePath = "default.png"; // À remplacer par un FileChooser plus tard

    @FXML
    public void initialize() {
        // Met à jour le chiffre à côté du Slider en temps réel
        sldImpact.valueProperty().addListener((observable, oldValue, newValue) -> {
            lblImpactValue.setText(String.valueOf(newValue.intValue()));
        });
    }

    @FXML
    void handleAjouterProduit(ActionEvent event) {
        // 1. Vérification des champs vides
        if (cbNom.getValue() == null || tfQuantite.getText().isEmpty() || taDescription.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Champs obligatoires", "Veuillez remplir tous les champs.");
            return;
        }

        // 2. Validation de la longueur de la description (Contrainte scénario : min 10)
        if (taDescription.getText().length() < 10) {
            showAlert(Alert.AlertType.ERROR, "Description trop courte", "La description doit faire au moins 10 caractères.");
            return;
        }

        try {
            // 3. Conversion et création de l'objet
            int qte = Integer.parseInt(tfQuantite.getText());

            Produit p = new Produit();
            p.setNom(cbNom.getValue());
            p.setTypeMateriau(cbType.getValue());
            p.setEtat(cbEtat.getValue());
            p.setQuantite(qte);
            p.setOrigine(tfOrigine.getText());
            p.setDescription(taDescription.getText());
            p.setImage(imagePath);
            p.setImpactEcologique((int) sldImpact.getValue());
            p.setArtisanId(3); // On utilise l'ID 3 par défaut pour les tests

            // 4. Appel au service
            sp.ajouter(p);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le produit recyclable a été publié !");
            viderChamps();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur format", "La quantité doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Impossible d'ajouter : " + e.getMessage());
        }
    }

    @FXML
    void handleUploadImage(ActionEvent event) {
        // Pour l'instant on simule, on ajoutera le FileChooser après
        this.imagePath = "produit_recyclable.jpg";
        System.out.println("Image sélectionnée");
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        viderChamps();
    }

    private void viderChamps() {
        tfQuantite.clear();
        tfOrigine.clear();
        taDescription.clear();
        cbNom.getSelectionModel().clearSelection();
        sldImpact.setValue(50);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}