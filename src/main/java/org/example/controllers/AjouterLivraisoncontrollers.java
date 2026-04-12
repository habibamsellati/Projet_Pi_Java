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
import org.example.services.livraisonServices;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
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

    // Flag pour savoir si on retourne au Front ou au Back
    private boolean isBackOffice = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
            e.printStackTrace();
        }
    }

    public void setLivraisonData(livraison l) {
        this.livraisonAModifier = l;
        if (labelTitre != null) labelTitre.setText("Modifier la Livraison");
        btnCreer.setText("Enregistrer les modifications");
        adresseField.setText(l.getAddressLivraison());
        if (l.getDateLivraison() != null) {
            datePicker.setValue(l.getDateLivraison().toLocalDate());
        }
        commandeCombo.setValue(l.getCommandeId());
        try {
            String emailLivreur = ls.getEmailById(l.getLivreurId());
            livreurCombo.setValue(emailLivreur);
        } catch (SQLException e) {
            System.err.println("Erreur récupération email livreur: " + e.getMessage());
        }
    }

    @FXML
    void handleCreer(ActionEvent event) {
        if (champsInvalides()) return;
        try {
            if (livraisonAModifier == null) {
                livraison newL = new livraison();
                remplirObjet(newL);
                ls.insertOne(newL);
                afficherAlerte("Succès", "Livraison ajoutée avec succès !", Alert.AlertType.INFORMATION);
            } else {
                remplirObjet(livraisonAModifier);
                ls.updateOne(livraisonAModifier);
                afficherAlerte("Succès", "Livraison mise à jour avec succès !", Alert.AlertType.INFORMATION);
            }
            retourListe(event);
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Base de données : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void remplirObjet(livraison l) throws SQLException {
        l.setAddressLivraison(adresseField.getText().trim());
        l.setDateLivraison(datePicker.getValue().atStartOfDay());
        l.setCommandeId(commandeCombo.getValue());
        String selectedEmail = livreurCombo.getValue();
        if (selectedEmail != null) {
            int idLivreur = ls.getIdByEmail(selectedEmail);
            l.setLivreurId(idLivreur);
        }
        if (livraisonAModifier == null) {
            l.setStatutLivraison("En attente");
        }
    }

    private boolean champsInvalides() {
        if (adresseField.getText().trim().isEmpty() || datePicker.getValue() == null ||
                commandeCombo.getValue() == null || livreurCombo.getValue() == null) {
            afficherAlerte("Champs manquants", "Veuillez remplir tout le formulaire.", Alert.AlertType.WARNING);
            return true;
        }
        return false;
    }

    @FXML
    void retourListe(ActionEvent event) {
        try {
            String fxmlFile;

            // --- CORRECTION DES CHEMINS ---
            if (isBackOffice) {
                fxmlFile = "/Dashboard.fxml"; // Nom corrigé selon tes fichiers
            } else {
                fxmlFile = "/AfficherLivraison.fxml";
            }

            // Sécurité pour éviter le NullPointerException
            URL url = getClass().getResource(fxmlFile);
            if (url == null) {
                System.err.println("ERREUR : Fichier introuvable -> " + fxmlFile);
                return;
            }

            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}