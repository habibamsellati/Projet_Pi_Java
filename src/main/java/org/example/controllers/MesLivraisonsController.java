package org.example.controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.Livraison;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceLivraison;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MesLivraisonsController {

    @FXML private Label lblCount;
    @FXML private Label lblTitre;
    @FXML private Button btnAjouter;
    @FXML private TextField tfRecherche;
    @FXML private VBox vboxLivraisons;

    private final ServiceLivraison service = new ServiceLivraison();
    private final List<Livraison> allLivraisons = new ArrayList<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private User currentUser;

    @FXML
    void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Session", "Utilisateur non connecte.");
            btnAjouter.setDisable(true);
            return;
        }

        lblTitre.setText("Gestion des Livraisons");
        btnAjouter.setVisible(currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.RESPONSABLE
                || currentUser.getRole() == Role.LIVREUR);
        btnAjouter.setManaged(btnAjouter.isVisible());

        tfRecherche.textProperty().addListener((obs, oldV, newV) -> renderList());
        chargerDonnees();
    }

    @FXML
    void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterLivraison.fxml"));
            Parent root = loader.load();
            AjouterLivraisonController controller = loader.getController();
            controller.setOnSaved(this::chargerDonnees);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvelle Livraison");
            stage.setScene(new Scene(root, 1050, 720));
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ajout livraison", e.getMessage());
        }
    }

    @FXML
    void handleRafraichir() {
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            Role role = currentUser.getRole();
            List<Livraison> list;

            if (role == Role.CLIENT) {
                list = service.afficherParClient(currentUser.getId());
            } else if (role == Role.LIVREUR) {
                list = service.afficherParLivreur(currentUser.getId());
            } else if (role == Role.ARTISANT) {
                list = service.afficherPourArtisan(currentUser.getId());
            } else if (role == Role.ADMIN || role == Role.RESPONSABLE) {
                list = service.afficherTout();
            } else {
                list = List.of();
            }

            allLivraisons.clear();
            allLivraisons.addAll(list);
            renderList();
        } catch (SQLException e) {
            showError("Chargement livraisons", e.getMessage());
        }
    }

    private void renderList() {
        String q = tfRecherche.getText() == null ? "" : tfRecherche.getText().trim().toLowerCase(Locale.ROOT);
        vboxLivraisons.getChildren().clear();

        int count = 0;
        for (Livraison l : allLivraisons) {
            String flat = (valueOrDash(l.getNumeroCommande()) + " " + valueOrDash(l.getStatut()) + " "
                    + valueOrDash(l.getTrackingCode()) + " " + valueOrDash(l.getAdresseLivraison())).toLowerCase(Locale.ROOT);
            if (q.isBlank() || flat.contains(q)) {
                vboxLivraisons.getChildren().add(createCard(l));
                count++;
            }
        }
        lblCount.setText(String.valueOf(count));
    }

    private HBox createCard(Livraison l) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setStyle("-fx-background-color:#f2f1f3; -fx-background-radius:20;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 12, 0, 0, 3);");

        VBox left = new VBox(8);
        Label date = new Label(l.getCreatedAt() == null ? "-" : l.getCreatedAt().toLocalDateTime().format(dateFormatter));
        date.setStyle("-fx-font-size:15; -fx-font-weight:bold; -fx-text-fill:#23150f;");
        Label adresse = new Label("🔎 " + valueOrDash(l.getAdresseLivraison()));
        adresse.setStyle("-fx-font-size:13; -fx-text-fill:#556b83;");
        left.getChildren().addAll(date, adresse);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label badge = new Label(formatStatusText(l.getStatut()));
        badge.setStyle(statusStyle(l.getStatut()));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(48);

        Button btnMap = new Button("📍");
        btnMap.setStyle("-fx-background-color:#d8e6d9; -fx-background-radius:12; -fx-cursor:hand;"
                + "-fx-min-width:36; -fx-min-height:36; -fx-font-size:15;");
        btnMap.setTooltip(new Tooltip("Voir la map"));
        btnMap.setOnAction(e -> openMapWindow(l));
        actions.getChildren().add(btnMap);

        boolean canEdit = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.RESPONSABLE;
        if (canEdit && "en_attente".equals(l.getStatut())) {
            Button btnEdit = new Button("✎");
            btnEdit.setStyle("-fx-background-color:#dcecf8; -fx-background-radius:12; -fx-cursor:hand;");
            btnEdit.setOnAction(e -> quickSetStatus(l, "en_cours"));

            Button btnDelete = new Button("🗑");
            btnDelete.setStyle("-fx-background-color:#ffe8ec; -fx-background-radius:12; -fx-cursor:hand;");
            btnDelete.setOnAction(e -> deleteLivraison(l));

            actions.getChildren().addAll(btnEdit, btnDelete);
        }

        card.getChildren().addAll(left, badge, actions);
        return card;
    }

    private void quickSetStatus(Livraison l, String newStatus) {
        try {
            service.updateStatut(l.getId(), newStatus);
            chargerDonnees();
        } catch (SQLException e) {
            showError("Statut", e.getMessage());
        }
    }

    private void deleteLivraison(Livraison l) {
        try {
            service.supprimer(l.getId());
            chargerDonnees();
        } catch (SQLException e) {
            showError("Suppression", e.getMessage());
        }
    }

    private void openMapWindow(Livraison l) {
        try {
            Stage stage = new Stage();
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            stage.setScene(new Scene(webView, 960, 600));
            stage.setTitle("Suivi Temps Reel - Livraison #" + l.getId());
            stage.show();

            String url = getClass().getResource("/map.html").toExternalForm();
            webEngine.load(url);
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    double destLat = 36.8065;
                    double destLng = 10.1815;
                    double livLat = destLat + (l.getId() % 10) * 0.002;
                    double livLng = destLng + (l.getId() % 7) * 0.002;
                    String safeAddress = valueOrDash(l.getAdresseLivraison()).replace("'", "\\'");
                    String script = String.format(Locale.US,
                            "updateTracking(%f, %f, '%s', %f, %f, '%s')",
                            destLat, destLng, safeAddress, livLat, livLng, "Calcul...");
                    webEngine.executeScript(script);
                }
            });
        } catch (Exception e) {
            showError("Carte", e.getMessage());
        }
    }

    private String formatStatusText(String status) {
        if (status == null) return "EN ATTENTE";
        return status.replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private String statusStyle(String status) {
        String s = status == null ? "" : status.toLowerCase(Locale.ROOT);
        String base = "-fx-padding: 5 12; -fx-background-radius: 14; -fx-font-weight:bold; -fx-font-size:12;";
        if (s.contains("livree")) {
            return base + "-fx-background-color:#d1f2df; -fx-text-fill:#0f6b42;";
        }
        if (s.contains("cours")) {
            return base + "-fx-background-color:#dbe3ff; -fx-text-fill:#3145b5;";
        }
        return base + "-fx-background-color:#f9e7b0; -fx-text-fill:#9a5409;";
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

