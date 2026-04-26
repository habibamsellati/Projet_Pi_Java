package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import org.example.models.Commande;
import org.example.services.ServiceCommande;

import java.util.function.UnaryOperator;

public class AjouterCommandeController {
    @FXML private TextField tfTotal;
    @FXML private TextField tfTelephone;
    @FXML private TextField tfClientId;
    @FXML private TextArea taAdresseLivraison;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<String> cbModePaiement;

    private final ServiceCommande service = new ServiceCommande();

    @FXML
    void initialize() {
        cbStatut.getItems().setAll("en_attente", "confirmee", "livree");
        cbModePaiement.getItems().setAll("carte", "espèces", "virement");
        cbStatut.setValue("en_attente");
        cbModePaiement.setValue("carte");
        configurerChampTotal();
        configurerChampClientId();
    }

    private void configurerChampTotal() {
        UnaryOperator<TextFormatter.Change> filtre = change -> {
            String texte = change.getControlNewText();
            if (texte.isEmpty() || texte.matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        };
        tfTotal.setTextFormatter(new TextFormatter<>(filtre));
    }

    private void configurerChampClientId() {
        UnaryOperator<TextFormatter.Change> filtre = change -> {
            String texte = change.getControlNewText();
            if (texte.isEmpty() || texte.matches("\\d*")) {
                return change;
            }
            return null;
        };
        tfClientId.setTextFormatter(new TextFormatter<>(filtre));
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            Commande commande = new Commande();
            commande.setAdresseLivraison(taAdresseLivraison.getText());
            commande.setTelephone(videVersNull(tfTelephone.getText()));
            commande.setModePaiement(cbModePaiement.getValue());
            commande.setStatut(cbStatut.getValue());
            commande.setTotal(tfTotal.getText() == null || tfTotal.getText().isBlank() ? null : Double.parseDouble(tfTotal.getText().trim()));
            commande.setClientId(tfClientId.getText() == null || tfClientId.getText().isBlank() ? null : Integer.parseInt(tfClientId.getText().trim()));

            service.ajouter(commande);
            basculerVersAffichage(event);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleVoirCommandes(ActionEvent event) {
        try {
            basculerVersAffichage(event);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleAnnuler() {
        tfTotal.clear();
        tfTelephone.clear();
        tfClientId.clear();
        taAdresseLivraison.clear();
        cbStatut.setValue("en_attente");
        cbModePaiement.setValue("carte");
    }

    private void basculerVersAffichage(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AfficherCommandes.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1100, 760));
        stage.setTitle("Artefact - Liste des Commandes");
        stage.show();
    }

    private String videVersNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

