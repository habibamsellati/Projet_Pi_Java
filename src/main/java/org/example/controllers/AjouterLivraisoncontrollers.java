package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.livraison;
import org.example.services.GeocodingService;
import org.example.services.livraisonServices;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterLivraisoncontrollers implements Initializable {

    @FXML private Label labelTitre;
    @FXML private DatePicker datePicker;
    @FXML private TextField adresseField;
    @FXML private ComboBox<Integer> commandeCombo;
    @FXML private ComboBox<String> livreurCombo;
    @FXML private Button btnCreer;

    private final livraisonServices ls = new livraisonServices();
    private livraison livraisonAModifier = null;
    private boolean isBackOffice = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuration par défaut (Mode Ajout)
        if (labelTitre != null) labelTitre.setText("Ajouter une Livraison");
        if (btnCreer != null) btnCreer.setText("+ Enregistrer la nouvelle livraison");

        chargerCombos();
    }

    public void setBackOffice(boolean value) {
        this.isBackOffice = value;
    }

    private void chargerCombos() {
        try {
            commandeCombo.setItems(FXCollections.observableArrayList(ls.getAllCommandeIds()));
            livreurCombo.setItems(FXCollections.observableArrayList(ls.getEmailsLivreurs()));
        } catch (SQLException e) {
            afficherAlerte("Erreur de chargement", "Impossible de charger : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Remplit le formulaire pour la MODIFICATION
     */
    public void setLivraisonData(livraison l) {
        if (l == null) return;
        this.livraisonAModifier = l;

        // --- CHANGEMENT DYNAMIQUE DE L'INTERFACE ---
        if (labelTitre != null) labelTitre.setText("Modification de Livraison");
        if (btnCreer != null) btnCreer.setText("Enregistrer les modifications");

        // --- REMPLISSAGE DES CHAMPS ---
        adresseField.setText(l.getAddressLivraison());
        if (l.getDateLivraison() != null) {
            datePicker.setValue(l.getDateLivraison().toLocalDate());
        }
        commandeCombo.setValue(l.getCommandeId());

        // Récupération sécurisée de l'email du livreur
        if (l.getLivreurId() != null && l.getLivreurId() != 0) {
            try {
                String emailLivreur = ls.getEmailById(l.getLivreurId());
                livreurCombo.setValue(emailLivreur);
            } catch (SQLException e) {
                System.err.println("Erreur récupération email livreur: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleCreer(ActionEvent event) {
        if (validationSaisie()) return;

        try {
            if (livraisonAModifier == null) {
                // MODE INSERTION
                livraison newL = new livraison();
                remplirObjet(newL);
                ls.insertOne(newL);
                afficherAlerte("Succès", "Livraison ajoutée avec succès !", Alert.AlertType.INFORMATION);
            } else {
                // MODE MISE À JOUR
                remplirObjet(livraisonAModifier);
                ls.updateOne(livraisonAModifier);
                afficherAlerte("Succès", "Livraison mise à jour avec succès !", Alert.AlertType.INFORMATION);
            }
            retourListe(event);
        } catch (SQLException e) {
            afficherAlerte("Erreur DB", e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void remplirObjet(livraison l) throws SQLException {
        String adresse = adresseField.getText().trim();
        l.setAddressLivraison(adresse);
        l.setDateLivraison(datePicker.getValue().atStartOfDay());
        l.setCommandeId(commandeCombo.getValue());

        String selectedEmail = livreurCombo.getValue();
        if (selectedEmail != null) {
            l.setLivreurId(ls.getIdByEmail(selectedEmail));
        }

        // Géocodage
        double[] coords = GeocodingService.getCoordinates(adresse);
        if (coords != null) {
            l.setLat(coords[0]);
            l.setLng(coords[1]);
        }

        if (livraisonAModifier == null) {
            l.setStatutLivraison("En attente");
        }
    }

    private boolean validationSaisie() {
        StringBuilder sb = new StringBuilder();
        if (adresseField.getText().trim().isEmpty()) sb.append("- Adresse vide.\n");
        if (datePicker.getValue() == null) sb.append("- Date manquante.\n");
        if (commandeCombo.getValue() == null) sb.append("- Commande non sélectionnée.\n");

        if (sb.length() > 0) {
            afficherAlerte("Données Invalides", sb.toString(), Alert.AlertType.WARNING);
            return true;
        }
        return false;
    }

    @FXML
    void retourListe(ActionEvent event) {
        try {
            String fxmlFile = isBackOffice ? "/Dashboard.fxml" : "/AfficherLivraison.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}