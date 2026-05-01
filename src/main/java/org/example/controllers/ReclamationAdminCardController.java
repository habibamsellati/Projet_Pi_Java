package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import java.awt.Desktop;
import java.net.URI;
import org.example.models.Reclamation;
import org.example.models.ReponseReclamation;
import org.example.models.User;
import org.example.services.EmailService;
import org.example.services.ReclamationSummaryService;
import org.example.services.ServiceReclamation;
import org.example.utils.SessionManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ReclamationAdminCardController {
    @FXML private Label lblTitre;
    @FXML private Label lblStatut;
    @FXML private Label lblDate;
    @FXML private Label lblClient;
    @FXML private Label lblDescription;
    @FXML private TextArea taReponse;
    @FXML private ComboBox<String> cbNouveauStatut;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnAjouterReponse;
    @FXML private Button btnModifierReponse;

    private Reclamation reclamation;
    private ReclamationsAdminController parentController;
    private ServiceReclamation serviceReclamation;
    private ReclamationSummaryService summaryService;
    private EmailService emailService;

    @FXML
    void initialize() {
        serviceReclamation = new ServiceReclamation();
        summaryService = new ReclamationSummaryService();
        emailService = new EmailService();

        // Initialiser combo box des statuts
        cbNouveauStatut.setItems(javafx.collections.FXCollections.observableArrayList(
                "en_attente",
                "en_cours",
                "resolu",
                "rejete"
        ));
    }

    public void setReclamation(Reclamation rec) {
        this.reclamation = rec;
        lblTitre.setText(rec.getTitre());
        lblDate.setText(rec.getFormattedDate());
        lblStatut.setText(rec.getStatut().substring(0, 1).toUpperCase() + 
                rec.getStatut().substring(1).replace("_", " "));

        // Appliquer style au badge de statut
        String statusClass = "status-" + rec.getStatut().toLowerCase().replace("_", "-");
        lblStatut.getStyleClass().addAll("status-badge", statusClass);

        if (rec.getClient() != null) {
            lblClient.setText("Client: " + rec.getClient().getNomComplet() + " (" + rec.getClient().getEmail() + ")");
        } else {
            lblClient.setText("Client: Inconnu");
        }

        if (rec.getDescription() != null && !rec.getDescription().isEmpty()) {
            lblDescription.setText(rec.getDescription());
        } else {
            lblDescription.setText("Pas de description");
            lblDescription.setStyle("-fx-text-fill: #999;");
        }

        cbNouveauStatut.setValue(rec.getStatut());
    }

    public void setParentController(ReclamationsAdminController parent) {
        this.parentController = parent;
    }

    @FXML
    void handleChangerStatut(ActionEvent event) {
        String nouveauStatut = cbNouveauStatut.getValue();
        try {
            serviceReclamation.modifierStatut(reclamation.getId(), nouveauStatut);
            showInfo("Succès", "Statut mis à jour");
            reclamation.setStatut(nouveauStatut);
            lblStatut.setText(nouveauStatut.substring(0, 1).toUpperCase() + 
                    nouveauStatut.substring(1).replace("_", " "));
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    void handleAjouterReponse(ActionEvent event) {
        String contenu = taReponse.getText().trim();
        if (contenu.isEmpty()) {
            showError("Erreur", "La réponse ne peut pas être vide");
            return;
        }

        try {
            User admin = SessionManager.getCurrentUser();
            if (admin == null) {
                showError("Erreur", "Session invalide. Veuillez vous reconnecter.");
                return;
            }
            ReponseReclamation rep = new ReponseReclamation(contenu, reclamation.getId(), admin.getId());
            serviceReclamation.ajouterReponse(rep);
            taReponse.clear();

            // Envoyer email de notification au client
            String emailClient = (reclamation.getClient() != null) ? reclamation.getClient().getEmail() : null;
            if (emailClient != null && !emailClient.isBlank()) {
                String nomClient = reclamation.getClient().getNomComplet();
                String nomAdmin  = admin.getNomComplet();
                emailService.envoyerReponseReclamation(emailClient, nomClient,
                        reclamation.getTitre(), contenu, nomAdmin);
                showInfo("Succès", "Réponse ajoutée et email envoyé au client.");
            } else {
                showInfo("Succès", "Réponse ajoutée (email client introuvable, notification non envoyée).");
            }

        } catch (IllegalArgumentException e) {
            showError("Erreur de validation", e.getMessage());
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    @FXML
    void handleModifierReponse(ActionEvent event) {
        String contenu = taReponse.getText() == null ? "" : taReponse.getText().trim();
        if (contenu.isEmpty()) {
            showError("Erreur", "Saisissez le nouveau contenu de la réponse.");
            return;
        }

        try {
            User admin = SessionManager.getCurrentUser();
            if (admin == null) {
                showError("Erreur", "Session invalide. Veuillez vous reconnecter.");
                return;
            }
            serviceReclamation.modifierDerniereReponse(reclamation.getId(), admin.getId(), contenu);
            showInfo("Succès", "Réponse modifiée.");
        } catch (IllegalArgumentException e) {
            showError("Modification impossible", e.getMessage());
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    @FXML
    void handleVoirReponses(ActionEvent event) {
        try {
            List<ReponseReclamation> reponses = serviceReclamation.obtenirReponses(reclamation.getId());
            StringBuilder sb = new StringBuilder("Réponses à cette réclamation:\n\n");
            for (ReponseReclamation rep : reponses) {
                sb.append("Admin: ").append(rep.getAdmin() != null ? rep.getAdmin().getNomComplet() : "Inconnu")
                  .append(" (").append(rep.getFormattedDate()).append(")\n")
                  .append(rep.getContenu()).append("\n\n");
            }
            if (reponses.isEmpty()) {
                sb.append("Aucune réponse pour le moment");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Réponses");
            alert.setHeaderText(null);
            TextArea ta = new TextArea(sb.toString());
            ta.setWrapText(true);
            ta.setEditable(false);
            alert.getDialogPane().setContent(ta);
            alert.showAndWait();
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    void handleVoirResume(ActionEvent event) {
        if (reclamation == null) {
            showError("Erreur", "Reclamation introuvable.");
            return;
        }

        try {
            ReclamationSummaryService.SummaryInsights insights = summaryService.analyzeReclamationOnly(reclamation);
            String digest = summaryService.buildReclamationOnlyDigest(reclamation);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Resume IA - Reclamation #" + reclamation.getId());
            alert.setHeaderText(null);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/reclamations-style.css").toExternalForm());

            ButtonType copySummaryBtn = new ButtonType("Copier resume court", ButtonBar.ButtonData.LEFT);
            ButtonType copyDigestBtn = new ButtonType("Copier digest complet", ButtonBar.ButtonData.LEFT);
            alert.getButtonTypes().setAll(copySummaryBtn, copyDigestBtn, ButtonType.CLOSE);

            VBox root = new VBox(12);
            root.getStyleClass().add("ai-summary-root");

            Label title = new Label("Resume IA - Reclamation #" + reclamation.getId());
            title.getStyleClass().add("ai-summary-title");

            HBox chips = new HBox(8);
            chips.setAlignment(Pos.CENTER_LEFT);
            Label urgenceChip = new Label("Urgence: " + insights.urgence());
            urgenceChip.getStyleClass().addAll("ai-chip", chipClassForUrgence(insights.urgence()));
            Label categorieChip = new Label("Categorie: " + insights.categorie());
            categorieChip.getStyleClass().addAll("ai-chip", "ai-chip-categorie");
            Label sourceChip = new Label("Source: Description reclamation");
            sourceChip.getStyleClass().addAll("ai-chip", "ai-chip-source");
            chips.getChildren().addAll(urgenceChip, categorieChip, sourceChip);

            VBox shortSummaryBox = new VBox(6);
            shortSummaryBox.getStyleClass().add("ai-summary-card");
            Label shortSummaryTitle = new Label("Resume court");
            shortSummaryTitle.getStyleClass().add("ai-section-title");
            Label shortSummaryText = new Label(insights.shortSummary());
            shortSummaryText.setWrapText(true);
            shortSummaryText.getStyleClass().add("ai-section-text");
            shortSummaryBox.getChildren().addAll(shortSummaryTitle, shortSummaryText);

            VBox keyPointsBox = new VBox(6);
            keyPointsBox.getStyleClass().add("ai-summary-card");
            Label pointsTitle = new Label("Points cles");
            pointsTitle.getStyleClass().add("ai-section-title");
            VBox pointsList = new VBox(4);
            for (String p : insights.pointsCles()) {
                Label item = new Label("• " + p);
                item.setWrapText(true);
                item.getStyleClass().add("ai-section-text");
                pointsList.getChildren().add(item);
            }
            keyPointsBox.getChildren().addAll(pointsTitle, pointsList);

            VBox actionBox = new VBox(6);
            actionBox.getStyleClass().add("ai-summary-card");
            Label actionTitle = new Label("Prochaine action recommandee");
            actionTitle.getStyleClass().add("ai-section-title");
            Label actionText = new Label(insights.prochaineAction());
            actionText.setWrapText(true);
            actionText.getStyleClass().add("ai-section-text");
            actionBox.getChildren().addAll(actionTitle, actionText);

            TextArea digestArea = new TextArea(digest);
            digestArea.setWrapText(true);
            digestArea.setEditable(false);
            digestArea.setPrefRowCount(10);
            digestArea.getStyleClass().add("ai-summary-body");
            VBox.setVgrow(digestArea, Priority.ALWAYS);

            root.getChildren().addAll(title, chips, shortSummaryBox, keyPointsBox, actionBox, digestArea);
            alert.getDialogPane().setContent(root);
            alert.getDialogPane().setPrefSize(900, 760);

            ButtonType clicked = alert.showAndWait().orElse(ButtonType.CLOSE);
            if (clicked == copySummaryBtn) {
                ClipboardContent content = new ClipboardContent();
                content.putString(insights.shortSummary());
                Clipboard.getSystemClipboard().setContent(content);
                showInfo("Copie", "Le resume court a ete copie dans le presse-papiers.");
            } else if (clicked == copyDigestBtn) {
                ClipboardContent content = new ClipboardContent();
                content.putString(digest);
                Clipboard.getSystemClipboard().setContent(content);
                showInfo("Copie", "Le digest complet a ete copie dans le presse-papiers.");
            }
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    void handleDemarrerMeet(ActionEvent event) {
        try {
            if (!Desktop.isDesktopSupported()) {
                showError("Meet", "Ouverture du navigateur non supportee sur ce poste.");
                return;
            }

            String roomId = "afkart-reclamation-" + reclamation.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            String meetUrl = "https://meet.jit.si/" + roomId;

            Desktop.getDesktop().browse(URI.create(meetUrl));

            String emailClient = (reclamation.getClient() != null) ? reclamation.getClient().getEmail() : null;
            String nomClient = (reclamation.getClient() != null) ? reclamation.getClient().getNomComplet() : "Client";

            if (emailClient == null || emailClient.isBlank()) {
                showError("Meet", "Email client introuvable. Impossible d'envoyer l'invitation.");
                return;
            }

            boolean sent = emailService.envoyerInvitationMeetReclamation(
                    emailClient,
                    nomClient,
                    reclamation.getTitre(),
                    meetUrl
            );

            if (sent) {
                showInfo("Meet", "Visio ouverte et invitation email envoyee au client.");
            } else {
                showError("Meet", "Visio ouverte, mais email non envoye. Verifiez email.properties (smtp.password).");
            }
        } catch (Exception e) {
            showError("Meet", "Impossible d'ouvrir Meet: " + e.getMessage());
        }
    }

    private String chipClassForUrgence(ReclamationSummaryService.Urgence urgence) {
        return switch (urgence) {
            case CRITIQUE -> "ai-chip-critique";
            case ELEVEE -> "ai-chip-elevee";
            case MOYENNE -> "ai-chip-moyenne";
            default -> "ai-chip-basse";
        };
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

