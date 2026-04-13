package org.example.controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.models.Article;
import org.example.models.Commentaire;
import org.example.models.Commande;
import org.example.models.LigneCommandeArticle;
import org.example.services.ServiceArticle;
import org.example.services.ServiceCommandeEcommerce;
import org.example.services.ServiceCommentaire;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ModuleEcommerceV1Controller {

    @FXML
    private TableView<Article> tvArticles;
    @FXML
    private TableColumn<Article, Integer> colArtId;
    @FXML
    private TableColumn<Article, String> colTitre;
    @FXML
    private TableColumn<Article, Double> colPrix;
    @FXML
    private TableColumn<Article, String> colCat;
    @FXML
    private TableColumn<Article, String> colDate;

    @FXML
    private TextField tfArtTitre;
    @FXML
    private TextArea taArtContenu;
    @FXML
    private TextField tfArtPrix;
    @FXML
    private TextField tfArtCat;
    @FXML
    private TextField tfArtisanId;

    @FXML
    private TextField tfCmdClientId;
    @FXML
    private TextField tfCmdArtisanId;
    @FXML
    private TextArea taPanierLignes;
    @FXML
    private TextField tfCmdAdresse;
    @FXML
    private TextField tfCmdMode;
    @FXML
    private TextField tfCmdTel;
    @FXML
    private TextArea taCmdClient;
    @FXML
    private TextArea taCmdArtisan;
    @FXML
    private TextField tfCmdIdStatut;
    @FXML
    private TextField tfCmdNouveauStatut;

    @FXML
    private TextField tfComArticleId;
    @FXML
    private ListView<String> lvCom;
    @FXML
    private TextArea taComContenu;

    /** Connexion BDD différée : sinon le FXML échoue si MySQL est arrêté. */
    private ServiceArticle serviceArticle;
    private ServiceCommandeEcommerce serviceCommande;
    private ServiceCommentaire serviceCommentaire;
    private boolean servicesReady;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int DEMO_USER_COMMENT = 3;

    @FXML
    public void initialize() {
        colArtId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colCat.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colDate.setCellValueFactory(c -> {
            var d = c.getValue().getDatePublication();
            return new ReadOnlyStringWrapper(d == null ? "-" : d.format(DF));
        });
        Platform.runLater(() -> ensureServices());
    }

    /**
     * Initialise les services au premier usage. Réessaie à chaque clic si MySQL n’était pas prêt.
     */
    private boolean ensureServices() {
        if (servicesReady) {
            return true;
        }
        try {
            ServiceArticle a = new ServiceArticle();
            ServiceCommandeEcommerce c = new ServiceCommandeEcommerce();
            ServiceCommentaire cm = new ServiceCommentaire();
            serviceArticle = a;
            serviceCommande = c;
            serviceCommentaire = cm;
            servicesReady = true;
            return true;
        } catch (RuntimeException e) {
            serviceArticle = null;
            serviceCommande = null;
            serviceCommentaire = null;
            alert(Alert.AlertType.ERROR, messageMysqlIndisponible(e));
            return false;
        }
    }

    private static String messageMysqlIndisponible(Throwable e) {
        return """
                Impossible de se connecter à MySQL.

                Vérifiez :
                • Que le serveur MySQL est démarré (Windows : services.msc → MySQL ou MariaDB).
                • Host/port (défaut localhost:3306) et le nom de la base (DB_NAME ou piprojet1).
                • DB_USER / DB_PWD (variables d’environnement ou -D sur la ligne de commande).

                Détail technique : %s
                """.formatted(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    }

    @FXML
    void handleArticlesRefresh() {
        if (!ensureServices()) {
            return;
        }
        try {
            tvArticles.setItems(FXCollections.observableArrayList(serviceArticle.listerCatalogue()));
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleArticleAjouter() {
        if (!ensureServices()) {
            return;
        }
        try {
            int aid = parseInt(tfArtisanId.getText(), "Artisan user id");
            Article a = new Article();
            a.setTitre(tfArtTitre.getText());
            a.setContenu(taArtContenu.getText());
            a.setPrix(Double.parseDouble(tfArtPrix.getText().trim().replace(',', '.')));
            a.setCategorie(tfArtCat.getText());
            a.setArtisanId(aid);
            a.setUserId(aid);
            serviceArticle.ajouter(a);
            alert(Alert.AlertType.INFORMATION, "Article créé (id=" + a.getId() + ").");
            handleArticlesRefresh();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleArticleModifier() {
        if (!ensureServices()) {
            return;
        }
        Article sel = tvArticles.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.WARNING, "Sélectionnez une ligne.");
            return;
        }
        try {
            int artisanUid = parseInt(tfArtisanId.getText(), "Artisan user id");
            Article a = new Article();
            a.setId(sel.getId());
            a.setTitre(tfArtTitre.getText());
            a.setContenu(taArtContenu.getText());
            a.setPrix(Double.parseDouble(tfArtPrix.getText().trim().replace(',', '.')));
            a.setCategorie(tfArtCat.getText());
            a.setArtisanId(artisanUid);
            a.setUserId(artisanUid);
            serviceArticle.modifier(a, artisanUid);
            alert(Alert.AlertType.INFORMATION, "Article mis à jour.");
            handleArticlesRefresh();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleArticleSupprimer() {
        if (!ensureServices()) {
            return;
        }
        Article sel = tvArticles.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.WARNING, "Sélectionnez une ligne.");
            return;
        }
        try {
            serviceArticle.supprimer(sel.getId());
            alert(Alert.AlertType.INFORMATION, "Article supprimé.");
            handleArticlesRefresh();
        } catch (IllegalStateException e) {
            alert(Alert.AlertType.WARNING, e.getMessage());
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleCommandeCreer() {
        if (!ensureServices()) {
            return;
        }
        try {
            int client = parseInt(tfCmdClientId.getText(), "Client id");
            List<LigneCommandeArticle> lignes = parsePanier(taPanierLignes.getText());
            Commande c = serviceCommande.creerDepuisPanier(
                    client,
                    tfCmdAdresse.getText(),
                    tfCmdMode.getText(),
                    tfCmdTel.getText(),
                    lignes
            );
            alert(Alert.AlertType.INFORMATION,
                    "Commande " + c.getId() + " — total " + String.format("%.2f", c.getTotal()) + " — N° " + c.getNumero());
            handleCommandeListerClient();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleCommandeListerClient() {
        if (!ensureServices()) {
            return;
        }
        try {
            int client = parseInt(tfCmdClientId.getText(), "Client id");
            StringBuilder sb = new StringBuilder();
            for (Commande c : serviceCommande.listerPourClient(client)) {
                sb.append(formatCommande(c)).append("\n");
            }
            taCmdClient.setText(sb.isEmpty() ? "(vide)" : sb.toString());
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleCommandeListerArtisan() {
        if (!ensureServices()) {
            return;
        }
        try {
            int artisan = parseInt(tfCmdArtisanId.getText(), "Artisan id");
            StringBuilder sb = new StringBuilder();
            for (Commande c : serviceCommande.listerPourArtisan(artisan)) {
                sb.append(formatCommande(c)).append("\n");
            }
            taCmdArtisan.setText(sb.isEmpty() ? "(vide)" : sb.toString());
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleCommandeStatut() {
        if (!ensureServices()) {
            return;
        }
        try {
            int id = parseInt(tfCmdIdStatut.getText(), "Commande id");
            String st = tfCmdNouveauStatut.getText();
            serviceCommande.mettreAJourStatut(id, st);
            alert(Alert.AlertType.INFORMATION, "Statut mis à jour.");
            handleCommandeListerClient();
            handleCommandeListerArtisan();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleCommandeAnnuler() {
        if (!ensureServices()) {
            return;
        }
        try {
            int id = parseInt(tfCmdIdStatut.getText(), "Commande id");
            int client = parseInt(tfCmdClientId.getText(), "Client id");
            serviceCommande.annulerCommandeClient(id, client);
            alert(Alert.AlertType.INFORMATION, "Commande annulée.");
            handleCommandeListerClient();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleComCharger() {
        if (!ensureServices()) {
            return;
        }
        try {
            tfComArticleId.setText(tfComArticleId.getText().trim());
            rafraichirCommentaires();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleComPublier() {
        if (!ensureServices()) {
            return;
        }
        try {
            int articleId = parseInt(tfComArticleId.getText(), "Article id");
            Commentaire c = new Commentaire();
            c.setArticleId(articleId);
            c.setUserId(DEMO_USER_COMMENT);
            c.setContenu(taComContenu.getText());
            serviceCommentaire.ajouter(c);
            taComContenu.clear();
            rafraichirCommentaires();
            alert(Alert.AlertType.INFORMATION, "Commentaire publié.");
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void handleComSupprimer() {
        if (!ensureServices()) {
            return;
        }
        try {
            String selected = lvCom.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.startsWith("#")) {
                throw new IllegalArgumentException("Sélectionnez une ligne commençant par #id.");
            }
            int end = selected.indexOf(' ');
            int cid = Integer.parseInt(selected.substring(1, end < 0 ? selected.length() : end));
            serviceCommentaire.supprimer(cid);
            rafraichirCommentaires();
            alert(Alert.AlertType.INFORMATION, "Supprimé.");
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void rafraichirCommentaires() throws SQLException {
        int articleId = parseInt(tfComArticleId.getText(), "Article id");
        List<Commentaire> list = serviceCommentaire.afficherParArticle(articleId);
        lvCom.setItems(FXCollections.observableArrayList(
                list.stream().map(c -> {
                    String d = c.getDatePub() == null ? "-" : c.getDatePub().format(DF);
                    return "#" + c.getId() + "  u" + c.getUserId() + "  " + d + "  —  " + c.getContenu();
                }).toList()
        ));
    }

    private static String formatCommande(Commande c) {
        String n = c.getNumero() != null ? c.getNumero() : "-";
        String m = c.getModePaiement() != null ? c.getModePaiement() : "-";
        return "id=" + c.getId() + " | N°" + n + " | user=" + c.getUserId()
                + " | total=" + String.format("%.2f", c.getTotal()) + " | " + c.getStatut()
                + " | " + m + " | " + c.getAdresseLivraison();
    }

    private static List<LigneCommandeArticle> parsePanier(String text) {
        List<LigneCommandeArticle> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Saisissez au moins une ligne panier.");
        }
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split(",");
            if (p.length != 2) {
                throw new IllegalArgumentException("Ligne invalide (attendu: idArticle,quantité) : " + line);
            }
            int aid = Integer.parseInt(p[0].trim());
            int q = Integer.parseInt(p[1].trim());
            list.add(new LigneCommandeArticle(aid, q));
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Panier vide.");
        }
        return list;
    }

    private static int parseInt(String s, String label) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private static void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg == null ? "" : msg).show();
    }
}
