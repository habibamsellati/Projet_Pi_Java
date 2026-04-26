package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.models.Reclamation;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceReclamation;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.File;
import java.sql.SQLException;

public class AjouterReclamationController {
    @FXML private TextField tfTitre;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea taDescription;
    @FXML private TextField tfImageUrl;
    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;
    @FXML private Label lblError;

    private ServiceReclamation serviceReclamation;
    private User currentUser;

    @FXML
    void initialize() {
        serviceReclamation = new ServiceReclamation();
        currentUser = SessionManager.getCurrentUser();

        if (currentUser == null || (currentUser.getRole() != Role.CLIENT && currentUser.getRole() != Role.ARTISANT)) {
            lblError.setText("Accès refusé");
            btnAjouter.setDisable(true);
        }

        // Ajouter des listeners pour la validation en temps réel
        comboType.getItems().addAll(
                "Reclamation technique",
                "Probleme de commande",
                "Autre"
        );
        comboType.valueProperty().addListener((obs, oldVal, newVal) -> validerChamps());
        tfTitre.textProperty().addListener((obs, oldVal, newVal) -> validerChamps());
        taDescription.textProperty().addListener((obs, oldVal, newVal) -> validerChamps());
        tfImageUrl.textProperty().addListener((obs, oldVal, newVal) -> validerChamps());
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            String typeSelectionne = comboType.getValue();
            if (typeSelectionne == null || typeSelectionne.isBlank()) {
                lblError.setText("Veuillez choisir un type de réclamation");
                lblError.setStyle("-fx-text-fill: #f59f00;");
                return;
            }

            // Créer une nouvelle réclamation
            Reclamation rec = new Reclamation();
            rec.setTitre(tfTitre.getText().trim());
            
            String description = taDescription.getText().trim();
            if (!description.isEmpty()) {
                rec.setDescription(description);
            }

            String imageUrl = tfImageUrl.getText().trim();
            if (!imageUrl.isEmpty()) {
                rec.setImageUrl(imageUrl);
            }

            rec.setClientId(currentUser.getId());

            // Ajouter via le service
            serviceReclamation.ajouterReclamation(rec);

            // La valeur sélectionnée est bien récupérée au clic sur Valider.
            System.out.println("Type choisi : " + typeSelectionne);

            showInfo("Succès", "Réclamation créée avec succès!");
            
            naviguerVersListeOuDashboard(event);
        } catch (IllegalArgumentException e) {
            lblError.setText(e.getMessage());
            lblError.setStyle("-fx-text-fill: #c0392b;");
        } catch (SQLException e) {
            lblError.setText("Erreur SQL: " + e.getMessage());
            lblError.setStyle("-fx-text-fill: #c0392b;");
        } catch (Exception e) {
            lblError.setText("Erreur: " + e.getMessage());
            lblError.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    @FXML
    void handleChoisirImage() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir une image");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp")
            );

            Window window = tfImageUrl.getScene() != null ? tfImageUrl.getScene().getWindow() : null;
            File selectedFile = chooser.showOpenDialog(window);
            if (selectedFile != null) {
                tfImageUrl.setText(selectedFile.toURI().toString());
                lblError.setText("");
            }
        } catch (Exception e) {
            showError("Erreur", "Impossible de sélectionner l'image : " + e.getMessage());
        }
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        try {
            naviguerVersListeOuDashboard(event);
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void naviguerVersListeOuDashboard(ActionEvent event) throws Exception {
        if (currentUser != null && currentUser.getRole() == Role.ARTISANT) {
            SceneNavigator.navigate(event, "/fxml/ReclamationsClient.fxml",
                    "Mes Réclamations", 1000, 700);
            return;
        }
        SceneNavigator.navigate(event, "/fxml/ReclamationsClient.fxml",
                "Mes Réclamations", 1000, 700);
    }

    private void validerChamps() {
        String typeSelectionne = comboType.getValue();
        String titre = tfTitre.getText().trim();
        String description = taDescription.getText().trim();
        String imageUrl = tfImageUrl.getText().trim();

        boolean typeValide = typeSelectionne != null && !typeSelectionne.isBlank();
        boolean titreValide = titre.length() >= 3 && titre.length() <= 255;
        boolean descriptionValide = description.isEmpty() || description.length() >= 10;
        boolean imageValide = imageUrl.isEmpty() || isImageUrl(imageUrl);

        btnAjouter.setDisable(!(typeValide && titreValide && descriptionValide && imageValide));

        // Afficher les erreurs
        lblError.setText("");
        if (!typeValide) {
            lblError.setText("Veuillez choisir un type de réclamation");
            lblError.setStyle("-fx-text-fill: #f59f00;");
        } else if (!titreValide) {
            lblError.setText("Le titre doit contenir entre 3 et 255 caractères");
            lblError.setStyle("-fx-text-fill: #f59f00;");
        } else if (!descriptionValide) {
            lblError.setText("La description doit contenir au moins 10 caractères");
            lblError.setStyle("-fx-text-fill: #f59f00;");
        } else if (!imageValide) {
            lblError.setText("Choisissez un fichier image valide ou saisissez une URL valide (.jpg, .png, .gif, .webp)");
            lblError.setStyle("-fx-text-fill: #f59f00;");
        }
    }

    private boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") 
                || lower.endsWith(".gif") || lower.endsWith(".webp")
                || lower.contains(".jpg?") || lower.contains(".jpeg?") 
                || lower.contains(".png?") || lower.contains(".gif?")
                || lower.contains(".webp?");
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

