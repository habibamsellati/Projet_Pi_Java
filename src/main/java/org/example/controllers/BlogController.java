package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.PanierService;
import org.example.services.ServiceArticle;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.File;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BlogController implements Initializable {

    @FXML private GridPane featuredGrid;
    @FXML private GridPane allGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> authorCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button btnPanier;
    @FXML private Button btnAjouterArticle;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private final PanierService panierService = PanierService.getInstance();
    private List<Article> articles = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initCombos();
        loadData();
        renderFeatured(articles);
        renderAll(articles);
        refreshPanierButton();

        User currentUser = SessionManager.getCurrentUser();
        boolean canCreateArticle = currentUser != null
                && (currentUser.getRole() == Role.ARTISANT || currentUser.getRole() == Role.ADMIN);
        if (btnAjouterArticle != null) {
            btnAjouterArticle.setVisible(canCreateArticle);
            btnAjouterArticle.setManaged(canCreateArticle);
        }
    }

    private void initCombos() {
        categoryCombo.getItems().addAll("Toutes les catégories", "Artisanat", "Décoration", "Textile", "Céramique", "Autres");
        categoryCombo.getSelectionModel().selectFirst();

        authorCombo.getItems().addAll("Tous les auteurs", "Artisan #1", "Artisan #2", "Artisan #3");
        authorCombo.getSelectionModel().selectFirst();

        sortCombo.getItems().addAll("Date (récent → ancien)", "Date (ancien → récent)", "Prix ↑", "Prix ↓", "Titre (A-Z)");
        sortCombo.getSelectionModel().selectFirst();
    }

    private void loadData() {
        try {
            articles = serviceArticle.afficher();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement articles : " + e.getMessage()).showAndWait();
            articles = new ArrayList<>();
        }
    }

    private void renderFeatured(List<Article> source) {
        featuredGrid.getChildren().clear();
        List<Article> list = source.subList(0, Math.min(2, source.size()));
        for (int i = 0; i < list.size(); i++) {
            featuredGrid.add(makeCard(list.get(i), 240), i, 0);
        }
    }

    private void renderAll(List<Article> source) {
        allGrid.getChildren().clear();
        int col = 0;
        int row = 0;
        for (Article a : source) {
            allGrid.add(makeCard(a, 190), col, row);
            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox makeCard(Article a, double imgHeight) {
        VBox card = new VBox();
        card.getStyleClass().add("article-card");
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane imgZone = new StackPane();
        imgZone.setPrefHeight(imgHeight);
        imgZone.setMinHeight(imgHeight);
        imgZone.setMaxHeight(imgHeight);

        Image image = loadImageSafely(a.getImageUrl());
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(9999);
            iv.setFitHeight(imgHeight);
            iv.setPreserveRatio(false);
            imgZone.getChildren().add(iv);
        } else {
            Region ph = makePlaceholder(imgHeight, a.getId() % 3 == 0);
            imgZone.getChildren().add(ph);
        }

        Region overlay = new Region();
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlay.getStyleClass().add("card-img-overlay");
        overlay.setMouseTransparent(true);

        VBox textOnImg = new VBox(3);
        textOnImg.setAlignment(Pos.BOTTOM_LEFT);
        textOnImg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        textOnImg.setPadding(new Insets(0, 14, 14, 14));
        textOnImg.setMouseTransparent(true);

        Label dateL = new Label(formatDate(a));
        dateL.getStyleClass().add("card-date");

        Label titleL = new Label(a.getTitre());
        titleL.getStyleClass().add("card-title-img");
        titleL.setWrapText(true);

        textOnImg.getChildren().addAll(dateL, titleL);
        imgZone.getChildren().addAll(overlay, textOnImg);
        StackPane.setAlignment(textOnImg, Pos.BOTTOM_LEFT);

        VBox body = new VBox(7);
        body.getStyleClass().add("card-body");
        body.setPadding(new Insets(14, 16, 16, 16));

        Label author = new Label("Auteur: Artisan #" + a.getArtisanId());
        author.getStyleClass().add("card-author");

        Label excerpt = new Label(safeExcerpt(a.getContenu()));
        excerpt.getStyleClass().add("card-excerpt");
        excerpt.setWrapText(true);

        VBox priceBox = new VBox();
        if (a.getPrix() != null) {
            Label price = new Label(String.format("%.2f DT", a.getPrix()));
            price.getStyleClass().add("card-price");
            priceBox.getChildren().add(price);
        }

        HBox btns = new HBox(10);
        btns.setPadding(new Insets(6, 0, 0, 0));

        Button btnLire = new Button("Lire la suite");
        btnLire.getStyleClass().add("btn-lire");
        HBox.setHgrow(btnLire, Priority.ALWAYS);
        btnLire.setMaxWidth(Double.MAX_VALUE);
        btnLire.setOnAction(e -> onLire(a));

        Button btnPanierCard = new Button("🛒  Ajouter au panier");
        btnPanierCard.getStyleClass().add("btn-panier-card");
        HBox.setHgrow(btnPanierCard, Priority.ALWAYS);
        btnPanierCard.setMaxWidth(Double.MAX_VALUE);
        updatePanierButton(btnPanierCard, a);
        btnPanierCard.setOnAction(e -> onAjouter(a, btnPanierCard));

        btns.getChildren().addAll(btnLire, btnPanierCard);
        body.getChildren().addAll(author, excerpt, priceBox, btns);

        card.getChildren().addAll(imgZone, body);
        return card;
    }

    private Region makePlaceholder(double h, boolean golden) {
        Region r = new Region();
        r.setMaxSize(Double.MAX_VALUE, h);
        r.setPrefHeight(h);
        r.getStyleClass().add(golden ? "card-golden-bg" : "card-img-placeholder");
        return r;
    }

    @FXML
    void handleFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryCombo.getValue();
        String author = authorCombo.getValue();
        String sort = sortCombo.getValue();

        List<Article> filtered = articles.stream()
                .filter(a -> q.isBlank() || contains(a.getTitre(), q) || contains(a.getContenu(), q))
                .filter(a -> category == null || "Toutes les catégories".equals(category) || category.equalsIgnoreCase(a.getCategorie()))
                .filter(a -> author == null || "Tous les auteurs".equals(author) || author.equalsIgnoreCase("Artisan #" + a.getArtisanId()))
                .collect(Collectors.toList());

        applySort(filtered, sort);
        renderFeatured(filtered);
        renderAll(filtered);
    }

    @FXML
    void handleVoirPanier() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) btnPanier.getScene().getWindow();
            SceneNavigator.navigate(stage, "/fxml/PanierClient.fxml", "Mon Panier", 1000, 760);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleAjouterArticle() {
        try {
            Stage stage = (Stage) btnAjouterArticle.getScene().getWindow();
            SceneNavigator.navigate(stage, "/fxml/AjouterArticle.fxml", "Créer un article", 1000, 760);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void onLire(Article a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleCommentairesView.fxml"));
            Parent root = loader.load();

            ArticleCommentairesController controller = loader.getController();
            controller.setArticle(a);

            Stage owner = (Stage) btnPanier.getScene().getWindow();
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(a.getTitre() == null ? "Article" : a.getTitre());
            stage.setScene(new Scene(root, 920, 700));
            stage.setResizable(true);
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir l'article : " + e.getMessage()).showAndWait();
        }
    }

    private void onAjouter(Article a, Button btnPanierCard) {
        boolean ajoute = panierService.ajouter(a);
        if (ajoute) {
            updatePanierButton(btnPanierCard, a);
        }
        refreshPanierButton();
    }

    private void updatePanierButton(Button button, Article article) {
        boolean dejaAuPanier = panierService.contient(article);
        button.setDisable(dejaAuPanier);
        button.setText(dejaAuPanier ? "✓ Déjà au panier" : "🛒  Ajouter au panier");
    }

    private void refreshPanierButton() {
        btnPanier.setText(String.format("🛒  Voir le panier (%d) - Total: %.2f DT", panierService.getCount(), panierService.getTotal()));
    }

    private void applySort(List<Article> list, String tri) {
        if (tri == null) {
            return;
        }
        switch (tri) {
            case "Date (ancien → récent)" -> list.sort(Comparator.comparing(Article::getDatePublication, Comparator.nullsLast(Comparator.naturalOrder())));
            case "Prix ↑" -> list.sort(Comparator.comparing(a -> a.getPrix() == null ? Double.MAX_VALUE : a.getPrix()));
            case "Prix ↓" -> list.sort((a, b) -> Double.compare(b.getPrix() == null ? -1 : b.getPrix(), a.getPrix() == null ? -1 : a.getPrix()));
            case "Titre (A-Z)" -> list.sort(Comparator.comparing(a -> a.getTitre() == null ? "" : a.getTitre().toLowerCase(Locale.ROOT)));
            default -> list.sort((a, b) -> {
                if (a.getDatePublication() == null && b.getDatePublication() == null) return 0;
                if (a.getDatePublication() == null) return 1;
                if (b.getDatePublication() == null) return -1;
                return b.getDatePublication().compareTo(a.getDatePublication());
            });
        }
    }

    private String formatDate(Article article) {
        if (article.getDatePublication() == null) {
            return "DATE INCONNUE";
        }
        return article.getDatePublication().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH))
                .toUpperCase(Locale.ROOT);
    }

    private String safeExcerpt(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 95 ? text.substring(0, 95) + "..." : text;
    }

    private boolean contains(String source, String search) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(search);
    }

    private Image loadImageSafely(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        String raw = imagePath.trim();

        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:") || raw.startsWith("jar:")) {
            candidates.add(raw);
        }

        File localFile = new File(raw);
        if (localFile.exists()) {
            candidates.add(localFile.toURI().toString());
        }

        URL resource = getClass().getResource(raw.startsWith("/") ? raw : "/" + raw);
        if (resource != null) {
            candidates.add(resource.toExternalForm());
        }

        for (String candidate : candidates) {
            try {
                Image image = new Image(candidate, false);
                if (!image.isError()) {
                    return image;
                }
            } catch (Exception ignored) {
                // Ignore.
            }
        }
        return null;
    }
}

