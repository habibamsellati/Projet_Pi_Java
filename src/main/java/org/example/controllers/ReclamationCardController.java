package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.example.models.Reclamation;
import org.example.models.User;
import org.example.services.ServiceReclamation;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.util.Optional;

public class ReclamationCardController {
    @FXML private Label lblTitre;
    @FXML private Label lblStatut;
    @FXML private Label lblDate;
    @FXML private Label lblDescription;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private Reclamation reclamation;
    private ReclamationsClientController parentController;
    private ServiceReclamation serviceReclamation;

    @FXML
    void initialize() {
        serviceReclamation = new ServiceReclamation();
    }

    public void setReclamation(Reclamation rec) {
        this.reclamation = rec;
        lblTitre.setText(rec.getTitre());
        lblDate.setText(rec.getFormattedDate());
        lblStatut.setText(rec.getStatut().substring(0, 1).toUpperCase() + 
                rec.getStatut().substring(1).replace("_", " "));

        // Appliquer style au badge de statut
        String statusClass = "status-" + rec.getStatut().toLowerCase().replace("_", "-");
        lblStatut.getStyleClass().addAll("status-badge", statusClass);

        if (rec.getDescription() != null && !rec.getDescription().isEmpty()) {
            lblDescription.setText(rec.getDescription());
        } else {
            lblDescription.setText("Pas de description");
            lblDescription.setStyle("-fx-text-fill: #999;");
        }

        // Désactiver la modification si le statut n'est pas "en_attente"
        if (!rec.getStatut().equals("en_attente")) {
            btnModifier.setDisable(true);
        }
    }

    public void setParentController(ReclamationsClientController parent) {
        this.parentController = parent;
    }

    @FXML
    void handleVoir(ActionEvent event) {
        showInfo("Détails de la Réclamation",
                "Titre: " + reclamation.getTitre() + "\n" +
                "Statut: " + reclamation.getStatut() + "\n" +
                "Date: " + reclamation.getFormattedDate() + "\n" +
                "Description: " + (reclamation.getDescription() != null ? reclamation.getDescription() : "N/A"));
    }

    @FXML
    void handleModifier(ActionEvent event) {
        if (!reclamation.getStatut().equals("en_attente")) {
            showError("Erreur", "Seules les réclamations en attente peuvent être modifiées");
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Utilisateur non connecté.");
            return;
        }

        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle("Modifier la réclamation");
        dialog.setHeaderText("Modifier votre réclamation en attente");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField tfTitre = new TextField(reclamation.getTitre());
        TextArea taDescription = new TextArea(reclamation.getDescription() == null ? "" : reclamation.getDescription());
        taDescription.setPrefRowCount(5);
        TextField tfImage = new TextField(reclamation.getImageUrl() == null ? "" : reclamation.getImageUrl());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Titre :"), 0, 0);
        grid.add(tfTitre, 1, 0);
        grid.add(new Label("Description :"), 0, 1);
        grid.add(taDescription, 1, 1);
        grid.add(new Label("Image :"), 0, 2);
        grid.add(tfImage, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(tfTitre.getText() == null || tfTitre.getText().trim().length() < 3);

        tfTitre.textProperty().addListener((obs, oldVal, newVal) ->
                saveButton.setDisable(newVal == null || newVal.trim().length() < 3));

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                Reclamation updated = new Reclamation();
                updated.setId(reclamation.getId());
                updated.setClientId(currentUser.getId());
                updated.setStatut(reclamation.getStatut());
                updated.setDateCreation(reclamation.getDateCreation());
                updated.setTitre(tfTitre.getText());
                updated.setDescription(taDescription.getText());
                updated.setImageUrl(tfImage.getText());
                return updated;
            }
            return null;
        });

        Optional<Reclamation> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                serviceReclamation.modifierReclamation(result.get());
                showInfo("Succès", "Réclamation modifiée avec succès.");
                if (parentController != null) {
                    parentController.refreshReclamations();
                }
            } catch (IllegalArgumentException ex) {
                showError("Validation", ex.getMessage());
            } catch (SQLException ex) {
                showError("Erreur", ex.getMessage());
            }
        }
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cette réclamation ?");
        confirmation.setContentText("Cette action est irréversible.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceReclamation.supprimerReclamation(reclamation.getId());
                showInfo("Succès", "Réclamation supprimée");
                if (parentController != null) {
                    parentController.refreshReclamations();
                }
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

