package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.example.models.Article;
import org.example.services.PanierService;
import org.example.services.ServiceArticle;
import org.example.utils.SceneNavigator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ClientCatalogueController {
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbTri;
    @FXML private FlowPane fpArticles;
    @FXML private Button btnVoirPanier;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private final PanierService panierService = PanierService.getInstance();
    private List<Article> allArticles = new ArrayList<>();

    @FXML
    void initialize() {
        cbCategorie.getItems().setAll("Toutes les catégories", "Artisanat", "Décoration", "Textile", "Céramique", "Autres");
        cbTri.getItems().setAll("Date (récent -> ancien)", "Prix (croissant)", "Prix (décroissant)", "Titre (A-Z)");
        cbCategorie.setValue("Toutes les catégories");
        cbTri.setValue("Date (récent -> ancien)");

        chargerArticles();
        mettreAJourBoutonPanier();
    }

    @FXML
    void handleFiltrer() {
        afficherArticlesFiltres();
    }

    @FXML
    void handleVoirPanier() {
        try {
            SceneNavigator.navigate((javafx.stage.Stage) btnVoirPanier.getScene().getWindow(), "/fxml/PanierClient.fxml", "Mon Panier", 1000, 760);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void chargerArticles() {
        try {
            allArticles = serviceArticle.afficher();
            afficherArticlesFiltres();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement articles : " + e.getMessage()).showAndWait();
        }
    }

    private void afficherArticlesFiltres() {
        fpArticles.getChildren().clear();

        String recherche = tfRecherche.getText() == null ? "" : tfRecherche.getText().trim().toLowerCase(Locale.ROOT);
        String categorie = cbCategorie.getValue();
        String tri = cbTri.getValue();

        List<Article> filtered = allArticles.stream()
                .filter(a -> recherche.isBlank()
                        || contient(a.getTitre(), recherche)
                        || contient(a.getContenu(), recherche))
                .filter(a -> "Toutes les catégories".equals(categorie)
                        || categorie == null
                        || categorie.equalsIgnoreCase(a.getCategorie()))
                .collect(Collectors.toList());

        appliquerTri(filtered, tri);

        for (Article article : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleClientCard.fxml"));
                Node card = loader.load();

                ClientArticleCardController controller = loader.getController();
                controller.setData(article);
                controller.setArticleDejaAuPanier(panierService.contient(article));
                controller.setOnAjouterPanier(() -> {
                    boolean ajoute = panierService.ajouter(controller.getCurrentArticle());
                    if (ajoute) {
                        controller.setArticleDejaAuPanier(true);
                    }
                    mettreAJourBoutonPanier();
                });

                fpArticles.getChildren().add(card);
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur affichage carte : " + e.getMessage()).showAndWait();
                break;
            }
        }
    }

    private void appliquerTri(List<Article> articles, String tri) {
        if (tri == null) {
            return;
        }
        switch (tri) {
            case "Prix (croissant)" -> articles.sort(Comparator.comparing(a -> a.getPrix() == null ? Double.MAX_VALUE : a.getPrix()));
            case "Prix (décroissant)" -> articles.sort((a, b) -> Double.compare(b.getPrix() == null ? -1 : b.getPrix(), a.getPrix() == null ? -1 : a.getPrix()));
            case "Titre (A-Z)" -> articles.sort(Comparator.comparing(a -> a.getTitre() == null ? "" : a.getTitre().toLowerCase(Locale.ROOT)));
            default -> articles.sort((a, b) -> {
                if (a.getDatePublication() == null && b.getDatePublication() == null) return 0;
                if (a.getDatePublication() == null) return 1;
                if (b.getDatePublication() == null) return -1;
                return b.getDatePublication().compareTo(a.getDatePublication());
            });
        }
    }

    private boolean contient(String source, String search) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(search);
    }

    private void mettreAJourBoutonPanier() {
        btnVoirPanier.setText("Voir le panier (" + panierService.getCount() + ")");
    }
}

