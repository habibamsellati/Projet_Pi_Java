package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.services.ServiceArticle;

import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;

public class ModifierArticleController {

    @FXML private TextField tfTitre;
    @FXML private TextArea taContenu;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private TextField tfPrix;
    @FXML private TextField tfImage;

    private final ServiceArticle service = new ServiceArticle();
    private Article article;
    private Runnable onSaved;
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

    public void setArticle(Article article) {
        this.article = article;
        tfTitre.setText(article.getTitre() == null ? "" : article.getTitre());
        taContenu.setText(article.getContenu() == null ? "" : article.getContenu());
        cbCategorie.setValue(article.getCategorie() == null ? "Autres" : article.getCategorie());
        tfImage.setText(article.getImageUrl() == null ? "" : article.getImageUrl());

        // Formater le prix en anglais (point décimal) avec max 2 décimales
        // pour ne pas être rejeté par le TextFormatter (regex \d*(\.\d{0,2})?)
        if (article.getPrix() == null) {
            tfPrix.setText("");
        } else {
            // Supprimer le TextFormatter le temps de définir la valeur, puis le remettre
            tfPrix.setTextFormatter(null);
            tfPrix.setText(String.format(Locale.US, "%.2f", article.getPrix()));
            configurerChampPrix();
        }
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    void handleModifier() {
        try {
            String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
            String contenu = taContenu.getText() == null ? "" : taContenu.getText().trim();
            String categorie = cbCategorie.getValue();
            String image = tfImage.getText() == null ? "" : tfImage.getText().trim();

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

            article.setTitre(titre);
            article.setContenu(contenu);
            article.setCategorie(categorie);
            article.setPrix(prix);
            article.setImageUrl(image.isBlank() ? null : image);

            service.modifier(article);
            new Alert(Alert.AlertType.INFORMATION, "Article modifie avec succes !").show();

            if (onSaved != null) {
                onSaved.run();
            }
            closeWindow();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitre.getScene().getWindow();
        stage.close();
    }
}

