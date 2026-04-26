package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceArticle;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import java.io.File;
import java.util.Set;
import java.util.function.UnaryOperator;

public class AjouterArticleController {
    @FXML private TextField tfTitre, tfPrix;
    @FXML private TextArea taContenu;
    @FXML private ComboBox<String> cbCategorie;

    private String selectedImagePath = "";
    private final ServiceArticle service = new ServiceArticle();
    private static final Set<String> CATEGORIES_AUTORISEES = Set.of(
            "Artisanat", "Décoration", "Textile", "Céramique", "Autres"
    );

    @FXML
    void initialize() {
        cbCategorie.getSelectionModel().selectFirst();
        configurerChampPrix();
    }

    private void configurerChampPrix() {
        UnaryOperator<TextFormatter.Change> filtrePrix = change -> {
            String nouveauTexte = change.getControlNewText();
            if (nouveauTexte.isEmpty()) {
                return change;
            }

            // Autorise uniquement les nombres positifs avec 2 decimales max.
            if (nouveauTexte.matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        };

        tfPrix.setTextFormatter(new TextFormatter<>(filtrePrix));
    }

    @FXML
    void handleUploadImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.toURI().toString();
            // Optionnel: Afficher une miniature ici
        }
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null || (currentUser.getRole() != Role.ARTISANT && currentUser.getRole() != Role.ADMIN)) {
                throw new IllegalArgumentException("Accès refusé : seuls les artisans et admins peuvent créer un article.");
            }

            String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
            String contenu = taContenu.getText() == null ? "" : taContenu.getText().trim();
            String categorie = cbCategorie.getValue();

            if (titre.isBlank() || titre.length() < 3) {
                throw new IllegalArgumentException("Ce champ doit contenir au moins 3 caractères.");
            }
            if (titre.length() > 255) {
                throw new IllegalArgumentException("Le titre ne peut pas dépasser 255 caractères.");
            }
            if (contenu.isBlank() || contenu.length() < 10) {
                throw new IllegalArgumentException("Ce champ doit contenir au moins 10 caractères.");
            }
            if (categorie != null && !categorie.isBlank() && !CATEGORIES_AUTORISEES.contains(categorie)) {
                throw new IllegalArgumentException("Catégorie invalide.");
            }

            String prixText = tfPrix.getText() == null ? "" : tfPrix.getText().trim();
            Double prix = null;
            if (!prixText.isEmpty()) {
                prix = Double.parseDouble(prixText);
                if (prix < 0) {
                    throw new IllegalArgumentException("Le prix doit être positif ou nul.");
                }
            }

            Article a = new Article(
                    titre,
                    contenu,
                    categorie,
                    prix,
                    selectedImagePath.isBlank() ? null : selectedImagePath,
                    getArtisanId()
            );
            service.ajouter(a);
            basculerVersAffichage(event);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void basculerVersAffichage(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AfficherArticles.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Artefact - Liste des Articles");
        stage.show();
    }

    private int getArtisanId() {
        User user = SessionManager.getCurrentUser();
        return (user != null) ? user.getId() : 1;
    }

    @FXML
    void handleAnnuler() {
        tfTitre.clear();
        taContenu.clear();
        tfPrix.clear();
        cbCategorie.getSelectionModel().selectFirst();
        selectedImagePath = "";
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            User user = SessionManager.getCurrentUser();
            if (user != null && user.getRole() == Role.ADMIN) {
                SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art – Backoffice Admin", 1200, 780);
            } else {
                SceneNavigator.navigate(event, "/fxml/ArtisanDashboard.fxml", "Espace Artisan", 900, 620);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }
}
