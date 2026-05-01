package org.example.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.models.Produit;
import org.example.models.User;
import org.example.services.ImageIAService;
import org.example.services.ServiceProduit;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class AjouterProduitController {

    @FXML private ComboBox<String> cbNom;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbEtat;
    @FXML private TextField        tfQuantite;
    @FXML private TextField        tfOrigine;
    @FXML private TextField        tfImpact;
    @FXML private TextArea         taDescription;
    @FXML private Label            lblDate;

    // Détection intelligente
    @FXML private TextArea    taDetection;
    @FXML private VBox        vboxResultats;
    @FXML private HBox        hboxChips;
    @FXML private ProgressBar pbCategorie;
    @FXML private ProgressBar pbType;
    @FXML private ProgressBar pbEtat;
    @FXML private Label       lblPbCategorie;
    @FXML private Label       lblPbType;
    @FXML private Label       lblPbEtat;

    // Aperçu image IA
    @FXML private VBox      vboxApercu;
    @FXML private ImageView ivApercu;
    @FXML private Label     lblApercuPlaceholder;
    @FXML private Button    btnGenererImage;

    private final ServiceProduit  sp  = new ServiceProduit();
    private final ImageIAService  ia  = new ImageIAService();
    private final Random          rng = new Random();

    private String imagePath = "default.png";
    private int    produitId = 0;
    private int    artisanId = 0;

    // Résultats analyse
    private String detectedNom  = null;
    private String detectedType = null;
    private String detectedEtat = null;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    // ── Détection intelligente ────────────────────────────────────────────────

    @FXML
    void handleAnalyserIA(ActionEvent event) {
        if (taDetection == null || taDetection.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ vide", "Décrivez votre produit avant d'analyser.");
            return;
        }
        String texte = taDetection.getText().toLowerCase();

        double scoreNom = 0;
        if      (texte.contains("plastique") || texte.contains("bouteille")) { detectedNom = "Plastique"; scoreNom = 0.826; }
        else if (texte.contains("carton")    || texte.contains("boite"))     { detectedNom = "Carton";    scoreNom = 0.90; }
        else if (texte.contains("papier")    || texte.contains("journal"))   { detectedNom = "Papier";    scoreNom = 0.85; }
        else if (texte.contains("verre"))                                    { detectedNom = "Verre";     scoreNom = 0.88; }
        else if (texte.contains("metal")     || texte.contains("fer"))       { detectedNom = "Metal";     scoreNom = 0.80; }
        else if (texte.contains("bois")      || texte.contains("chaise"))    { detectedNom = "Bois";      scoreNom = 0.82; }
        else if (texte.contains("tissu")     || texte.contains("textile"))   { detectedNom = "Tissu";     scoreNom = 0.78; }

        double scoreType = 0;
        if      (texte.contains("naturel"))  { detectedType = "Naturel";    scoreType = 1.0; }
        else if (texte.contains("durable"))  { detectedType = "Durable";    scoreType = 0.90; }
        else if (texte.contains("ecolo"))    { detectedType = "Ecologique"; scoreType = 0.85; }

        double scoreEtat = 0;
        if      (texte.contains("excellent") || texte.contains("neuf"))  { detectedEtat = "Excellent";  scoreEtat = 0.95; }
        else if (texte.contains("bon")       || texte.contains("bonne")) { detectedEtat = "Bon";        scoreEtat = 0.80; }
        else if (texte.contains("moyen"))                                { detectedEtat = "Moyen";      scoreEtat = 0.70; }
        else if (texte.contains("casse")     || texte.contains("use"))   { detectedEtat = "A recycler"; scoreEtat = 0.75; }

        afficherResultats(scoreNom, scoreType, scoreEtat);
    }

    private void afficherResultats(double scoreNom, double scoreType, double scoreEtat) {
        pbCategorie.setProgress(scoreNom);
        pbType.setProgress(scoreType);
        pbEtat.setProgress(scoreEtat);
        lblPbCategorie.setText(String.format("%.1f%%", scoreNom * 100));
        lblPbType.setText(String.format("%.1f%%", scoreType * 100));
        lblPbEtat.setText(String.format("%.1f%%", scoreEtat * 100));

        hboxChips.getChildren().clear();
        if (detectedNom  != null) hboxChips.getChildren().add(makeChip("Catégorie : " + detectedNom,  "#e3f2fd", "#1565c0"));
        if (detectedType != null) hboxChips.getChildren().add(makeChip("Type : " + detectedType,      "#e8f5e9", "#2e7d32"));
        if (detectedEtat != null) hboxChips.getChildren().add(makeChip(detectedEtat,                  "#fff3e0", "#e65100"));

        vboxResultats.setVisible(true);
        vboxResultats.setManaged(true);
    }

    @FXML
    void handleAppliquerResultats(ActionEvent event) {
        if (detectedNom  != null && cbNom  != null) cbNom.setValue(detectedNom);
        if (detectedType != null && cbType != null) cbType.setValue(detectedType);
        if (detectedEtat != null && cbEtat != null) cbEtat.setValue(detectedEtat);
        if (taDetection  != null && taDescription != null && taDescription.getText().isBlank())
            taDescription.setText(taDetection.getText());
    }

    // ── Génération image IA ───────────────────────────────────────────────────

    @FXML
    void handleGenererImage(ActionEvent event) {
        String description = taDescription.getText();
        if (description == null || description.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Description vide",
                    "Remplissez la description avant de générer une image.");
            return;
        }

        // Désactiver le bouton pendant la génération
        if (btnGenererImage != null) {
            btnGenererImage.setDisable(true);
            btnGenererImage.setText("Génération...");
        }
        if (lblApercuPlaceholder != null) lblApercuPlaceholder.setText("Génération en cours...");

        int seed = rng.nextInt(99999);

        // Génération en arrière-plan pour ne pas bloquer l'UI
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return ia.genererUrlImage(description, seed);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            String url = task.getValue();
            if (url != null) {
                afficherImageApercu(url);
                imagePath = url; // sauvegarder l'URL comme image du produit
            } else {
                if (lblApercuPlaceholder != null)
                    lblApercuPlaceholder.setText("Échec de génération. Vérifiez votre connexion.");
            }
            if (btnGenererImage != null) {
                btnGenererImage.setDisable(false);
                btnGenererImage.setText("✦ Générer Image IA");
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (lblApercuPlaceholder != null)
                lblApercuPlaceholder.setText("Erreur : " + task.getException().getMessage());
            if (btnGenererImage != null) {
                btnGenererImage.setDisable(false);
                btnGenererImage.setText("✦ Générer Image IA");
            }
        }));

        new Thread(task, "image-ia-thread").start();
    }

    private void afficherImageApercu(String url) {
        try {
            Image img = new Image(url, 400, 300, true, true, true);
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                    Platform.runLater(() -> {
                        if (ivApercu != null) {
                            ivApercu.setImage(img);
                            ivApercu.setVisible(true);
                            ivApercu.setManaged(true);
                        }
                        if (lblApercuPlaceholder != null) {
                            lblApercuPlaceholder.setVisible(false);
                            lblApercuPlaceholder.setManaged(false);
                        }
                    });
                }
            });
        } catch (Exception e) {
            System.err.println("[AjouterProduit] Erreur affichage image : " + e.getMessage());
        }
    }

    // ── Enregistrement ────────────────────────────────────────────────────────

    @FXML
    void handleAjouterProduit(ActionEvent event) {
        if (cbNom.getValue() == null || tfQuantite.getText().isBlank() || taDescription.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Champs obligatoires", "Veuillez remplir tous les champs.");
            return;
        }
        if (taDescription.getText().length() < 10) {
            showAlert(Alert.AlertType.ERROR, "Description trop courte", "Au moins 10 caractères.");
            return;
        }
        try {
            int qte = Integer.parseInt(tfQuantite.getText().trim());
            int impact = 50;
            if (tfImpact != null && !tfImpact.getText().isBlank()) {
                try { impact = Integer.parseInt(tfImpact.getText().trim()); }
                catch (NumberFormatException ignored) {}
            }
            User currentUser = SessionManager.getCurrentUser();

            Produit p = new Produit();
            p.setId(produitId);
            p.setNom(cbNom.getValue());
            p.setTypeMateriau(cbType.getValue());
            p.setEtat(cbEtat.getValue());
            p.setQuantite(qte);
            p.setOrigine(tfOrigine.getText());
            p.setDescription(taDescription.getText());
            p.setImage(imagePath);
            p.setImpactEcologique(impact);
            p.setArtisanId(artisanId > 0 ? artisanId : (currentUser != null ? currentUser.getId() : 3));

            if (produitId > 0) {
                sp.modifier(p);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit modifié !");
            } else {
                sp.ajouter(p);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit ajouté !");
            }
            viderChamps();
            if (taDescription.getScene() != null)
                taDescription.getScene().getWindow().hide();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur format", "La quantité doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Impossible d'enregistrer : " + e.getMessage());
        }
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        viderChamps();
        if (taDescription != null && taDescription.getScene() != null)
            taDescription.getScene().getWindow().hide();
    }

    public void setProduitEnEdition(Produit produit) {
        if (produit == null) return;
        produitId = produit.getId();
        artisanId = produit.getArtisanId();
        imagePath = produit.getImage() != null ? produit.getImage() : "default.png";
        if (cbNom != null)         cbNom.setValue(produit.getNom());
        if (cbType != null)        cbType.setValue(produit.getTypeMateriau());
        if (cbEtat != null)        cbEtat.setValue(produit.getEtat());
        if (tfQuantite != null)    tfQuantite.setText(String.valueOf(produit.getQuantite()));
        if (tfOrigine != null)     tfOrigine.setText(produit.getOrigine() == null ? "" : produit.getOrigine());
        if (tfImpact != null)      tfImpact.setText(String.valueOf(produit.getImpactEcologique()));
        if (taDescription != null) taDescription.setText(produit.getDescription() == null ? "" : produit.getDescription());
        // Afficher l'image existante
        if (produit.getImage() != null && !produit.getImage().isBlank()
                && (produit.getImage().startsWith("http") || produit.getImage().startsWith("file"))) {
            afficherImageApercu(produit.getImage());
        }
    }

    private void viderChamps() {
        produitId = 0; artisanId = 0; imagePath = "default.png";
        if (cbNom != null)         cbNom.getSelectionModel().clearSelection();
        if (cbType != null)        cbType.getSelectionModel().clearSelection();
        if (cbEtat != null)        cbEtat.getSelectionModel().clearSelection();
        if (tfQuantite != null)    tfQuantite.clear();
        if (tfOrigine != null)     tfOrigine.clear();
        if (tfImpact != null)      tfImpact.clear();
        if (taDescription != null) taDescription.clear();
        if (taDetection != null)   taDetection.clear();
        if (vboxResultats != null) { vboxResultats.setVisible(false); vboxResultats.setManaged(false); }
        if (ivApercu != null)      { ivApercu.setImage(null); ivApercu.setVisible(false); ivApercu.setManaged(false); }
        if (lblApercuPlaceholder != null) {
            lblApercuPlaceholder.setText("Remplissez la description puis cliquez sur « Générer Image IA »");
            lblApercuPlaceholder.setVisible(true); lblApercuPlaceholder.setManaged(true);
        }
    }

    private Label makeChip(String text, String bg, String fg) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                      "-fx-background-radius:14;-fx-padding:4 10;-fx-font-size:11;-fx-font-weight:bold;");
        return chip;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }
}
