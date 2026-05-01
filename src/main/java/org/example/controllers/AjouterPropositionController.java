package org.example.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.models.Produit;
import org.example.models.Proposition;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ImageIAService;
import org.example.services.PropositionAIService;
import org.example.services.ServiceProduit;
import org.example.services.ServiceProposition;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AjouterPropositionController {

    @FXML private TextField        tfTitre;
    @FXML private TextArea         taDescription;
    @FXML private ComboBox<String> cbProduit;
    @FXML private TextField        tfPrix;
    @FXML private TextField        tfTelephone;
    @FXML private Label            lblDate;

    // Image IA
    @FXML private VBox      vboxApercu;
    @FXML private ImageView ivApercu;
    @FXML private Label     lblApercuPlaceholder;
    @FXML private Button    btnGenererImage;

    // Estimation prix IA
    @FXML private VBox        vboxEstimation;
    @FXML private Label       lblEstimDetail;
    @FXML private Button      btnAppliquerPrix;

    // Artisans recommandés
    @FXML private VBox vboxArtisans;
    @FXML private VBox listeArtisans;

    private final ServiceProposition sp  = new ServiceProposition();
    private final ServiceProduit     spd = new ServiceProduit();
    private final ImageIAService     ia  = new ImageIAService();
    private final PropositionAIService aiService = new PropositionAIService();
    private final Random             rng = new Random();

    private Proposition   propositionEnEdition = null;
    private List<Produit> produits;
    private String        imageUrl    = "generated_proposition.png";
    private double        prixEstime  = 0;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        User currentUser = SessionManager.getCurrentUser();
        boolean isClient = currentUser != null && currentUser.getRole() == Role.CLIENT;

        if (!isClient && vboxArtisans != null) {
            vboxArtisans.setVisible(false);
            vboxArtisans.setManaged(false);
        }

        try {
            produits = spd.afficher();
            for (Produit p : produits) {
                // Format : "Bois - Ecologique (Bon)"
                String label = nvl(p.getNomAffichage(), "Produit");
                if (p.getTypeMateriau() != null && !p.getTypeMateriau().isBlank())
                    label += " - " + p.getTypeMateriau();
                if (p.getEtat() != null && !p.getEtat().isBlank())
                    label += " (" + p.getEtat() + ")";
                cbProduit.getItems().add(label);
            }
            if (!cbProduit.getItems().isEmpty()) cbProduit.setValue(cbProduit.getItems().get(0));
        } catch (SQLException e) {
            System.err.println("Erreur chargement produits: " + e.getMessage());
        }

        // Quand le produit change → afficher bloc estimation + artisans
        cbProduit.valueProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                // Afficher le bloc estimation dès qu'un produit est sélectionné
                if (vboxEstimation != null) {
                    vboxEstimation.setVisible(true);
                    vboxEstimation.setManaged(true);
                }
                if (isClient) {
                    chargerArtisansRecommandes();
                }
            }
        });
    }

    // ── Génération image IA ───────────────────────────────────────────────────

    @FXML
    void handleGenererImage() {
        String description = taDescription.getText();
        if (description == null || description.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Description vide", "Remplissez la description d'abord.");
            return;
        }
        if (btnGenererImage != null) { btnGenererImage.setDisable(true); btnGenererImage.setText("Génération..."); }
        if (lblApercuPlaceholder != null) lblApercuPlaceholder.setText("Génération en cours...");

        int seed = rng.nextInt(99999);
        Task<String> task = new Task<>() {
            @Override protected String call() { return ia.genererUrlImage(description, seed); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            String url = task.getValue();
            if (url != null) { imageUrl = url; afficherApercu(url); }
            else if (lblApercuPlaceholder != null) lblApercuPlaceholder.setText("Échec. Vérifiez votre connexion.");
            if (btnGenererImage != null) { btnGenererImage.setDisable(false); btnGenererImage.setText("✦ Régénérer"); }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (btnGenererImage != null) { btnGenererImage.setDisable(false); btnGenererImage.setText("✦ Régénérer"); }
        }));
        new Thread(task, "image-ia-prop").start();
    }

    private void afficherApercu(String url) {
        try {
            Image img = new Image(url, 550, 155, true, true, true);
            img.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0 && !img.isError()) {
                    Platform.runLater(() -> {
                        if (ivApercu != null) { ivApercu.setImage(img); ivApercu.setVisible(true); ivApercu.setManaged(true); }
                        if (lblApercuPlaceholder != null) { lblApercuPlaceholder.setVisible(false); lblApercuPlaceholder.setManaged(false); }
                    });
                }
            });
        } catch (Exception e) { System.err.println("[AjouterProposition] " + e.getMessage()); }
    }

    // ── Estimation prix IA ────────────────────────────────────────────────────

    @FXML
    void handleEstimerPrix() {
        Produit produit = getProduitSelectionne();
        if (produit == null) {
            showAlert(Alert.AlertType.WARNING, "Produit", "Sélectionnez un produit d'abord.");
            return;
        }

        double base = calculerPrixBase(produit.getNom());
        double multiplicateur = calculerMultiplicateur(produit.getEtat(), produit.getTypeMateriau());
        prixEstime = Math.round(base * multiplicateur * 10.0) / 10.0;
        double min = Math.round(prixEstime * 0.75 * 10.0) / 10.0;
        double max = Math.round(prixEstime * 1.30 * 10.0) / 10.0;
        double confiance = 0.65 + rng.nextDouble() * 0.35;

        // Ligne résultat exactement comme dans la capture
        lblEstimDetail.setText(String.format(
            "Fourchette : %.1f - %.1f TND  |  Suggere : %.1f TND  |  Confiance : %.0f%%",
            min, max, prixEstime, confiance * 100));

        // Afficher le bouton Appliquer
        if (btnAppliquerPrix != null) {
            btnAppliquerPrix.setText("Appliquer ce prix");
            btnAppliquerPrix.setVisible(true);
            btnAppliquerPrix.setManaged(true);
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.CLIENT) {
            chargerArtisansRecommandes();
        }
    }

    @FXML
    void handleAppliquerPrix() {
        tfPrix.setText(String.format("%.2f", prixEstime));
        vboxEstimation.setVisible(false);
        vboxEstimation.setManaged(false);
    }

    private double calculerPrixBase(String nom) {
        if (nom == null) return 30;
        return switch (nom.toLowerCase()) {
            case "plastique" -> 25;
            case "verre"     -> 35;
            case "metal"     -> 45;
            case "bois"      -> 50;
            case "carton"    -> 20;
            case "papier"    -> 15;
            case "tissu"     -> 30;
            default          -> 30;
        };
    }

    private double calculerMultiplicateur(String etat, String type) {
        double mult = 1.0;
        if (etat != null) mult *= switch (etat.toLowerCase()) {
            case "excellent" -> 1.4;
            case "bon"       -> 1.2;
            case "moyen"     -> 1.0;
            default          -> 0.8;
        };
        if (type != null && (type.toLowerCase().contains("naturel") || type.toLowerCase().contains("ecolo")))
            mult *= 1.1;
        return Math.round(mult * 100.0) / 100.0;
    }

    // ── Artisans recommandés ──────────────────────────────────────────────────

    private void chargerArtisansRecommandes() {
        if (listeArtisans == null || vboxArtisans == null) return;
        listeArtisans.getChildren().clear();

        try {
            Produit produit = getProduitSelectionne();
            String materiau = produit != null ? produit.getNom() : null;

            List<PropositionAIService.ArtisanScore> artisans = aiService.recommanderArtisans(materiau);
            if (artisans.isEmpty()) {
                Label empty = new Label("Aucun artisan recommandé pour le moment.");
                empty.setStyle("-fx-font-size: 12; -fx-text-fill: #8f8a82;");
                listeArtisans.getChildren().add(empty);
            } else {
                int rang = 1;
                for (PropositionAIService.ArtisanScore artisan : artisans) {
                    listeArtisans.getChildren().add(buildArtisanRow(rang++, artisan));
                }
            }

            vboxArtisans.setVisible(true);
            vboxArtisans.setManaged(true);

        } catch (Exception e) {
            System.err.println("[ArtisansRecommandes] " + e.getMessage());
        }
    }

    private HBox buildArtisanRow(int rang, PropositionAIService.ArtisanScore artisan) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10 14;" +
                "-fx-border-color: #e0ddd5; -fx-border-radius: 8; -fx-border-width: 0.5;");

        Label lblRang = new Label(rang + ".");
        lblRang.setStyle("-fx-font-size: 13; -fx-font-weight: 500; -fx-text-fill: #888; -fx-min-width: 18;");

        Label avatar = new Label(nvl(artisan.initiales(), "??"));
        boolean positif = artisan.score() > 0;
        avatar.setStyle("-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32;" +
                "-fx-alignment: center; -fx-font-size: 12; -fx-font-weight: 500;" +
                "-fx-background-radius: 999;" +
                (positif
                        ? "-fx-background-color: #e8c97a; -fx-text-fill: #7a5200;"
                        : "-fx-background-color: #f1efe8; -fx-text-fill: #888;"));

        VBox infoBox = new VBox(2);
        Label lblNom = new Label(nvl(artisan.nom(), "Artisan"));
        lblNom.setStyle("-fx-font-size: 14; -fx-text-fill: #2f2430;");
        Label lblReason = new Label(nvl(artisan.matchReason(), ""));
        lblReason.setStyle("-fx-font-size: 11; -fx-text-fill: #8f8a82;");
        infoBox.getChildren().addAll(lblNom, lblReason);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        Label lblScore = new Label(artisan.score() + " pts");
        lblScore.setStyle("-fx-font-size: 12; -fx-background-radius: 999; -fx-padding: 2 10;" +
                (positif
                        ? "-fx-background-color: #fdf3d6; -fx-text-fill: #7a5200; -fx-border-color: #e8c97a; -fx-border-width: 0.5; -fx-border-radius: 999;"
                        : "-fx-background-color: #f1efe8; -fx-text-fill: #888; -fx-border-color: #ddd; -fx-border-width: 0.5; -fx-border-radius: 999;"));

        Label lblProps = new Label(artisan.nbPropositions() + " propositions");
        lblProps.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

        Tooltip.install(row, new Tooltip("Spécialités: " + nvl(artisan.specialites(), "Polyvalent")));
        row.getChildren().addAll(lblRang, avatar, infoBox, lblScore, lblProps);
        return row;
    }

    // ── Enregistrement ────────────────────────────────────────────────────────

    @FXML
    void handleEnregistrer() {
        if (tfTitre.getText().isBlank() || taDescription.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Champs obligatoires", "Titre et description sont requis.");
            return;
        }
        if (taDescription.getText().length() < 10) {
            showAlert(Alert.AlertType.ERROR, "Description trop courte", "Au moins 10 caractères.");
            return;
        }
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) { showAlert(Alert.AlertType.ERROR, "Session", "Vous devez être connecté."); return; }

            int produitId = getProduitIdSelectionne();
            if (produitId <= 0) { showAlert(Alert.AlertType.ERROR, "Produit", "Sélectionnez un produit."); return; }

            double prix = 0;
            if (!tfPrix.getText().isBlank()) {
                try { prix = Double.parseDouble(tfPrix.getText().replace(',', '.')); }
                catch (NumberFormatException e) { showAlert(Alert.AlertType.ERROR, "Prix", "Prix invalide."); return; }
            }

            Proposition p = propositionEnEdition != null ? propositionEnEdition : new Proposition();
            p.setTitre(tfTitre.getText().trim());
            p.setDescription(taDescription.getText().trim());
            p.setProduitId(produitId);
            p.setUserId(user.getId());
            p.setPrixPropose(prix);
            p.setTelephone(tfTelephone.getText().trim());
            p.setStatut("en_attente");
            p.setImage(imageUrl);

            if (propositionEnEdition != null) { sp.modifier(p); showAlert(Alert.AlertType.INFORMATION, "Succès", "Proposition modifiée !"); }
            else { sp.ajouter(p); showAlert(Alert.AlertType.INFORMATION, "Succès", "Proposition créée !"); }

            if (tfTitre.getScene() != null) tfTitre.getScene().getWindow().hide();
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage()); }
    }

    @FXML
    void handleAnnuler() {
        if (tfTitre.getScene() != null) tfTitre.getScene().getWindow().hide();
    }

    public void setPropositionEnEdition(Proposition p) {
        this.propositionEnEdition = p;
        if (p == null) return;
        tfTitre.setText(nvl(p.getTitre(), ""));
        taDescription.setText(nvl(p.getDescription(), ""));
        tfPrix.setText(String.valueOf(p.getPrixPropose()));
        tfTelephone.setText(nvl(p.getTelephone(), ""));
        if (produits != null) {
            for (int i = 0; i < produits.size(); i++) {
                if (produits.get(i).getId() == p.getProduitId()) {
                    cbProduit.getSelectionModel().select(i); break;
                }
            }
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private Produit getProduitSelectionne() {
        int idx = cbProduit.getSelectionModel().getSelectedIndex();
        if (idx < 0 || produits == null || idx >= produits.size()) return null;
        return produits.get(idx);
    }

    private int getProduitIdSelectionne() {
        Produit p = getProduitSelectionne();
        return p == null ? -1 : p.getId();
    }

    private Label makeChip(String text, String bg, String fg) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                      "-fx-background-radius:14;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");
        return chip;
    }

    private String nvl(String v, String def) { return (v == null || v.isBlank()) ? def : v; }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
