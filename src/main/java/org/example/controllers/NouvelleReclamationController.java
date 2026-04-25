package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import org.example.models.Reclamation;
import org.example.services.ServiceReclamation;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NouvelleReclamationController {
    @FXML
    private Label lblNom, lblPrenom, lblDate, lblImageName;
    @FXML
    private ComboBox<String> cbType;
    @FXML
    private TextArea taDescription;

    private String selectedImageName = "";

    @FXML
    public void initialize() {
        if (org.example.utils.SessionStore.isConnected()) {
            lblNom.setText(org.example.utils.SessionStore.getCurrentUser().getNom());
            lblPrenom.setText(org.example.utils.SessionStore.getCurrentUser().getPrenom());
        } else {
            lblNom.setText("Utilisateur");
            lblPrenom.setText("Anonyme");
        }
        lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")));
        cbType.getItems().addAll("Réclamation technique", "Problème de commande", "Autre");
    }

    @FXML
    void handleChoisirFichier(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(null);
        if (file != null) {
            selectedImageName = file.getName();
            lblImageName.setText(selectedImageName);
        }
    }

    @FXML
    void handleCreerReclamation(ActionEvent event) {
        try {
            String titre = cbType.getValue();
            String desc = taDescription.getText() == null ? "" : taDescription.getText().trim();
            if (titre == null || titre.isBlank()) {
                throw new IllegalArgumentException("Sélectionnez un type de réclamation.");
            }
            if (!desc.isBlank() && desc.length() < 10) {
                throw new IllegalArgumentException("La description doit contenir au moins 10 caractères.");
            }

            Reclamation r = new Reclamation(
                    org.example.utils.SessionStore.getUserId(),
                    titre,
                    desc,
                    selectedImageName,
                    ServiceReclamation.STATUT_EN_ATTENTE);
            ServiceReclamation service = new ServiceReclamation();
            service.ajouter(r);
            new Alert(Alert.AlertType.INFORMATION, "Réclamation créée avec succès.").show();
            handleReset();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleReset() {
        cbType.setValue(null);
        taDescription.clear();
        selectedImageName = "";
        lblImageName.setText("Aucun fichier choisi");
    }
}
