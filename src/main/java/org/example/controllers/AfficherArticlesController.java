package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.models.Article;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceArticle;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AfficherArticlesController {

    @FXML private VBox articlesContainer;
    @FXML private Label lblAucunArticle;
    // Formulaire d'ajout
    @FXML private TextField    tfNomArticle;
    @FXML private TextArea     taContenuArticle;
    @FXML private ComboBox<String> cbCategorieArticle;
    @FXML private TextField    tfPrixArticle;
    @FXML private TextField    tfImageArticle;
    // Recherche / tri
    @FXML private TextField        tfRechercheArticle;
    @FXML private ComboBox<String> cbTriArticles;

    private final ServiceArticle service = new ServiceArticle();
    private List<Article> allArticles = new ArrayList<>();

    @FXML
    void initialize() {
        cbTriArticles.getItems().setAll("Par défaut", "A -> Z", "Z -> A");
        cbTriArticles.setValue("Par défaut");
        cbCategorieArticle.getSelectionModel().selectFirst();

        tfRechercheArticle.textProperty().addListener((obs, o, n) -> appliquerRechercheEtTri());
        cbTriArticles.valueProperty().addListener((obs, o, n) -> appliquerRechercheEtTri());

        rafraichirListe();
    }

    @FXML
    void handleRafraichir() {
        rafraichirListe();
    }

    @FXML
    void handlePublierArticle() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null || (currentUser.getRole() != Role.ARTISANT && currentUser.getRole() != Role.ADMIN)) {
                throw new IllegalArgumentException("Accès refusé : seuls les artisans et admins peuvent publier un article.");
            }

            String titre   = tfNomArticle.getText() == null ? "" : tfNomArticle.getText().trim();
            String contenu = taContenuArticle.getText() == null ? "" : taContenuArticle.getText().trim();
            String categorie = cbCategorieArticle.getValue();
            String prixText = tfPrixArticle.getText() == null ? "" : tfPrixArticle.getText().trim();
            String image    = tfImageArticle.getText() == null ? "" : tfImageArticle.getText().trim();

            if (titre.length() < 3) {
                throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères.");
            }
            if (contenu.length() < 10) {
                throw new IllegalArgumentException("Le contenu doit contenir au moins 10 caractères.");
            }

            Double prix = null;
            if (!prixText.isBlank()) {
                prix = Double.parseDouble(prixText);
                if (prix < 0) throw new IllegalArgumentException("Le prix doit être positif ou nul.");
            }

            Article article = new Article();
            article.setTitre(titre);
            article.setContenu(contenu);
            article.setCategorie(categorie == null ? "Autres" : categorie);
            article.setPrix(prix);
            article.setImageUrl(image.isBlank() ? null : image);
            article.setArtisanId(currentUser.getId());

            service.ajouter(article);
            new Alert(Alert.AlertType.INFORMATION, "Article publié avec succès !").show();
            rafraichirListe();
            handleViderFormulaire();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Le prix doit être un nombre valide.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleViderFormulaire() {
        tfNomArticle.clear();
        taContenuArticle.clear();
        cbCategorieArticle.getSelectionModel().selectFirst();
        tfPrixArticle.clear();
        tfImageArticle.clear();
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            User user = SessionManager.getCurrentUser();
            if (user != null && user.getRole() == Role.ADMIN) {
                SceneNavigator.navigate(event, "/fxml/AdminDashboard.fxml", "afk'art – Backoffice Admin", 1200, 780);
            } else {
                SceneNavigator.navigate(event, "/fxml/MainView.fxml", "Artefact", 1200, 750);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void rafraichirListe() {
        try {
            allArticles = service.afficher();
            appliquerRechercheEtTri();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur SQL : " + e.getMessage()).show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur UI : " + e.getMessage()).show();
        }
    }

    private void appliquerRechercheEtTri() {
        articlesContainer.getChildren().clear();

        String keyword = tfRechercheArticle == null || tfRechercheArticle.getText() == null
                ? ""
                : tfRechercheArticle.getText().trim().toLowerCase(Locale.ROOT);

        List<Article> filtered = allArticles.stream()
                .filter(a -> matchRecherche(a, keyword))
                .toList();

        String tri = cbTriArticles == null ? "Par défaut" : cbTriArticles.getValue();
        if ("A -> Z".equals(tri)) {
            filtered = filtered.stream()
                    .sorted(Comparator.comparing(a -> safeTitre(a).toLowerCase(Locale.ROOT)))
                    .toList();
        } else if ("Z -> A".equals(tri)) {
            filtered = filtered.stream()
                    .sorted(Comparator.comparing((Article a) -> safeTitre(a).toLowerCase(Locale.ROOT)).reversed())
                    .toList();
        }

        for (Article article : filtered) {
            try {
                articlesContainer.getChildren().add(createArticleCard(article));
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur UI : " + e.getMessage()).show();
                break;
            }
        }
        updateEtatVide();
    }

    private boolean matchRecherche(Article article, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        String titre = article.getTitre() == null ? "" : article.getTitre().toLowerCase(Locale.ROOT);
        String contenu = article.getContenu() == null ? "" : article.getContenu().toLowerCase(Locale.ROOT);
        return titre.contains(keyword) || contenu.contains(keyword);
    }

    private String safeTitre(Article article) {
        return article.getTitre() == null ? "" : article.getTitre();
    }

    private Node createArticleCard(Article article) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleCard.fxml"));
        Node node = loader.load();

        ArticleCardController cardController = loader.getController();
        cardController.setData(article);
        cardController.setOnRefresh(this::rafraichirListe);
        return node;
    }

    private void updateEtatVide() {
        boolean isEmpty = articlesContainer.getChildren().isEmpty();
        lblAucunArticle.setVisible(isEmpty);
        lblAucunArticle.setManaged(isEmpty);
    }
}

