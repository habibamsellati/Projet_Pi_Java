package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.example.models.Livraison;
import org.example.services.ServiceLivraison;

import java.sql.SQLException;
import java.time.LocalDate;

public class AjouterLivraisonController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<ServiceLivraison.OptionItem> commandeCombo;
    @FXML private ComboBox<ServiceLivraison.OptionItem> livreurCombo;
    @FXML private TextField adresseField;
    @FXML private Label lblInfo;

    private final ServiceLivraison service = new ServiceLivraison();
    private Runnable onSaved;

    @FXML
    void initialize() {
        datePicker.setValue(LocalDate.now());

        try {
            commandeCombo.setItems(FXCollections.observableArrayList(service.getCommandesDisponibles()));
            livreurCombo.setItems(FXCollections.observableArrayList(service.getLivreurs()));
        } catch (SQLException e) {
            showError("Chargement", e.getMessage());
        }

        commandeCombo.valueProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                lblInfo.setText("Commande selectionnee: " + selected.toString());
            }
        });
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    void handleSave() {
        ServiceLivraison.OptionItem commande = commandeCombo.getValue();
        if (commande == null) {
            showError("Validation", "Selectionnez une commande.");
            return;
        }

        try {
            Integer clientId = service.findClientIdByCommande(commande.getId());
            if (clientId == null || clientId <= 0) {
                showError("Validation", "Client introuvable pour cette commande.");
                return;
            }

            Livraison l = new Livraison();
            l.setCommandeId(commande.getId());
            l.setClientId(clientId);
            l.setLivreurId(livreurCombo.getValue() == null ? null : livreurCombo.getValue().getId());
            l.setDateLivraison(datePicker.getValue());
            l.setAdresseLivraison(adresseField.getText());
            l.setStatut("en_attente");

            service.ajouter(l);
            if (onSaved != null) {
                onSaved.run();
            }
            close();
        } catch (Exception e) {
            showError("Ajout livraison", e.getMessage());
        }
    }

    @FXML
    void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) adresseField.getScene().getWindow();
        if (stage.getOwner() != null) {
            stage.close();
            return;
        }

        // Vue chargee dans MainView (StackPane contentArea): revenir a la liste au lieu de fermer l'app
        try {
            Parent listView = FXMLLoader.load(getClass().getResource("/fxml/LivraisonsView.fxml"));
            Parent parent = adresseField.getParent();
            while (parent != null && !(parent instanceof Pane)) {
                parent = parent.getParent();
            }
            if (parent instanceof Pane pane) {
                pane.getChildren().setAll(listView);
                return;
            }
        } catch (Exception ignored) {
            // fallback ci-dessous
        }

        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

