package org.example.controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class EspaceLivreurController {

    @FXML private VBox vboxMissions;
    @FXML private Label nomLivreur;

    private final livraisonServices service = new livraisonServices();
    private int idLivreurConnecte = 14;
    private Timeline gpsTimer;

    @FXML
    public void initialize() {
        if (nomLivreur != null) {
            nomLivreur.setText("Espace Livreur (ID: " + idLivreurConnecte + ")");
        }
        chargerMissionsLivreur();
    }

    @FXML
    void afficherPlanning(ActionEvent event) {
        Stage stage = new Stage();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        stage.setScene(new Scene(webView, 1050, 750));
        stage.setTitle("AfkArt - Planning & Météo Locale");
        stage.show();

        URL url = getClass().getResource("/planning.html");
        if (url == null) {
            System.err.println("ERREUR : planning.html introuvable !");
            return;
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    List<livraison> missions = service.findByLivreur(idLivreurConnecte);

                    // CONSTRUCTION DU JSON PROPRE
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < missions.size(); i++) {
                        livraison l = missions.get(i);

                        // Nettoyage de l'adresse pour JS
                        String addr = l.getAddressLivraison().replace("\"", "\\\"").replace("\n", " ");
                        // Format date ISO (YYYY-MM-DD)
                        String date = (l.getDateLivraison() != null) ? l.getDateLivraison().toLocalDate().toString() : "";

                        json.append("{");
                        json.append("\"addresslivraison\":\"").append(addr).append("\",");
                        json.append("\"datelivraison\":\"").append(date).append("\",");
                        json.append("\"statutlivraison\":\"").append(l.getStatutLivraison()).append("\",");

                        // Gestion sécurisée des coordonnées
                        json.append("\"lat\":").append(l.getLat() != null ? l.getLat() : "null").append(",");
                        json.append("\"lng\":").append(l.getLng() != null ? l.getLng() : "null");
                        json.append("}");

                        if (i < missions.size() - 1) json.append(",");
                    }
                    json.append("]");

                    String finalJson = json.toString();
                    System.out.println("JSON envoyé au calendrier : " + finalJson);

                    // Exécution de la fonction JS
                    webEngine.executeScript("loadMissions('" + finalJson + "')");

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        webEngine.load(url.toExternalForm());
    }

    private void chargerMissionsLivreur() {
        try {
            vboxMissions.getChildren().clear();
            List<livraison> missions = service.findByLivreur(idLivreurConnecte);

            if (missions.isEmpty()) {
                vboxMissions.getChildren().add(new Label("Aucune mission affectée."));
            } else {
                for (livraison l : missions) {
                    vboxMissions.getChildren().add(creerCarteMission(l));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox creerCarteMission(livraison l) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setMaxWidth(700);

        HBox header = new HBox();
        Label statusBadge = new Label("• " + l.getStatutLivraison());

        String styleBadge = "-fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;";
        if ("En cours".equalsIgnoreCase(l.getStatutLivraison())) {
            statusBadge.setStyle(styleBadge + "-fx-background-color: #E0E7FF; -fx-text-fill: #3730A3;");
        } else if ("Livré".equalsIgnoreCase(l.getStatutLivraison())) {
            statusBadge.setStyle(styleBadge + "-fx-background-color: #DCFCE7; -fx-text-fill: #166534;");
        } else {
            statusBadge.setStyle(styleBadge + "-fx-background-color: #FFF9E6; -fx-text-fill: #D4A017;");
        }

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label idLabel = new Label("#" + l.getId());
        idLabel.setStyle("-fx-text-fill: #DCDDE1; -fx-font-size: 20; -fx-font-weight: bold;");
        header.getChildren().addAll(statusBadge, spacer, idLabel);

        VBox info = new VBox(8);
        Label adresse = new Label("📍 " + l.getAddressLivraison());
        adresse.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2D3436;");
        Label date = new Label("📅 " + (l.getDateLivraison() != null ? l.getDateLivraison().toLocalDate().toString() : "Date à définir"));
        info.getChildren().addAll(adresse, date);

        Button btnAction = new Button();
        btnAction.setMaxWidth(Double.MAX_VALUE);
        String commonStyle = "-fx-background-radius: 15; -fx-padding: 12; -fx-font-weight: bold; -fx-cursor: hand;";

        if ("En cours".equals(l.getStatutLivraison())) {
            btnAction.setText("✅ Marquer comme Livré");
            btnAction.setStyle(commonStyle + "-fx-background-color: #27ae60; -fx-text-fill: white;");
            btnAction.setOnAction(e -> handleTerminer(l));
            lancerSuiviGPS();
        } else if ("Livré".equals(l.getStatutLivraison())) {
            btnAction.setText("✨ Mission Terminée");
            btnAction.setDisable(true);
            btnAction.setStyle(commonStyle + "-fx-background-color: #DCDDE1; -fx-text-fill: #7f8c8d;");
        } else {
            btnAction.setText("📦 Démarrer la mission");
            btnAction.setStyle(commonStyle + "-fx-background-color: #8D6E63; -fx-text-fill: white;");
            btnAction.setOnAction(e -> handleDemarrer(l));
        }

        card.getChildren().addAll(header, info, btnAction);
        return card;
    }

    private void handleDemarrer(livraison l) {
        try {
            service.demarrerMission(l.getId());
            chargerMissionsLivreur();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleTerminer(livraison l) {
        try {
            service.terminerMission(l.getId());
            chargerMissionsLivreur();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void lancerSuiviGPS() {
        if (gpsTimer != null) return;
        gpsTimer = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            double simuLat = 36.8065 + (Math.random() * 0.01);
            double simuLng = 10.1815 + (Math.random() * 0.01);
            try {
                service.updateLivreurPosition(idLivreurConnecte, simuLat, simuLng);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
        gpsTimer.setCycleCount(Animation.INDEFINITE);
        gpsTimer.play();
    }
}