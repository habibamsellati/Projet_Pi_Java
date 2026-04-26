package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.models.Commande;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.PanierService;
import org.example.services.ServiceCommande;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ValiderCommandeController {

    @FXML private VBox              vbRecap;
    @FXML private Label             lblTotalRecap;
    @FXML private TextField         tfNom;
    @FXML private TextField         tfPrenom;
    @FXML private TextField         tfTelephone;
    @FXML private TextArea          taAdresse;
    @FXML private ComboBox<String>  cbPaiement;

    private final PanierService    panierService    = PanierService.getInstance();
    private final ServiceCommande  serviceCommande  = new ServiceCommande();

    @FXML
    void initialize() {
        cbPaiement.getItems().setAll("carte", "espèces", "virement");
        cbPaiement.setValue("carte");

        // Pré-remplir nom/prénom depuis la session
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            tfNom.setText(user.getNom()    != null ? user.getNom()    : "");
            tfPrenom.setText(user.getPrenom() != null ? user.getPrenom() : "");
        }

        // Afficher le récapitulatif des articles
        vbRecap.getChildren().clear();
        for (Article article : panierService.getItems()) {
            vbRecap.getChildren().add(buildRecapCard(article));
        }
        lblTotalRecap.setText(String.format("Total : %.2f DT", panierService.getTotal()));
    }

    @FXML
    void handleRetour() {
        navigate("/fxml/PanierClient.fxml", "Mon Panier", 1000, 760);
    }

    @FXML
    void handleValider() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null || user.getRole() != Role.CLIENT) {
                throw new IllegalArgumentException("Session client invalide.");
            }
            if (panierService.getCount() == 0) {
                throw new IllegalArgumentException("Votre panier est vide.");
            }

            Commande commande = new Commande();
            commande.setAdresseLivraison(taAdresse.getText());
            String tel = tfTelephone.getText();
            commande.setTelephone(tel == null || tel.isBlank() ? null : tel.trim());
            commande.setModePaiement(cbPaiement.getValue());
            commande.setStatut("en_attente");
            commande.setClientId(user.getId());
            commande.setEmailClient(user.getEmail()); // email du client connecté
            String nomComplet = ((tfPrenom.getText() == null ? "" : tfPrenom.getText().trim()) + " " +
                    (tfNom.getText() == null ? "" : tfNom.getText().trim())).trim();
            commande.setNomClient(nomComplet.isBlank() ? user.getNomComplet() : nomComplet);

            for (Article a : panierService.getItems()) {
                commande.addArticle(a);
            }
            commande.setTotal(commande.calculateTotal());

            serviceCommande.ajouter(commande);
            panierService.vider();

            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "✅ Commande créée avec succès !\nNuméro : " + commande.getNumero());
            ok.setHeaderText(null);
            ok.showAndWait();

            navigate("/fxml/HistoriqueCommandes.fxml", "Historique des commandes", 1100, 760);

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // ── Construction des cartes récapitulatives ────────────────────────────

    private HBox buildRecapCard(Article article) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #F9F7F2; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 6, 0, 0, 2);");

        // Image
        ImageView iv = new ImageView();
        iv.setFitWidth(60);
        iv.setFitHeight(60);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setImage(loadImageSafely(article.getImageUrl()));
        Rectangle clip = new Rectangle(60, 60);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        iv.setClip(clip);

        // Info : titre + catégorie
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titre = new Label(article.getTitre() != null ? article.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #5D4037;");
        titre.setWrapText(true);
        titre.setMaxWidth(280);
        Label cat = new Label(article.getCategorie() != null ? article.getCategorie() : "");
        cat.setStyle("-fx-font-size: 12; -fx-text-fill: #A1887F;");
        info.getChildren().addAll(titre, cat);

        // Prix
        Label prix = new Label(article.getPrix() == null ? "— DT"
                : String.format("%.2f DT", article.getPrix()));
        prix.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #8D5E3C;");

        card.getChildren().addAll(iv, info, prix);
        return card;
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private void navigate(String fxml, String title, double w, double h) {
        try {
            Stage stage = (Stage) taAdresse.getScene().getWindow();
            SceneNavigator.navigate(stage, fxml, title, w, h);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // ── Chargement d'image ─────────────────────────────────────────────────

    private Image loadImageSafely(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        List<String> candidates = new ArrayList<>();
        String raw = imagePath.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:") || raw.startsWith("jar:")) {
            candidates.add(raw);
        }
        File f = new File(raw);
        if (f.exists()) candidates.add(f.toURI().toString());
        String cp = raw.startsWith("/") ? raw : "/" + raw;
        java.net.URL res = getClass().getResource(cp);
        if (res != null) candidates.add(res.toExternalForm());
        for (String c : candidates) {
            try {
                Image img = new Image(c, false);
                if (!img.isError()) return img;
            } catch (Exception ignored) {}
        }
        return null;
    }
}

