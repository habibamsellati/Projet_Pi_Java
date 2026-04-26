package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class AfficherLivraisoncontrollers {

    @FXML private VBox vboxLivraisons;
    private final livraisonServices service = new livraisonServices();

    @FXML
    public void initialize() {
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            vboxLivraisons.getChildren().clear();
            vboxLivraisons.setSpacing(15);
            // Récupère toutes les livraisons avec les coordonnées GPS actuelles des livreurs
            List<livraison> livraisons = service.findALL();

            for (livraison l : livraisons) {
                vboxLivraisons.getChildren().add(creerCarteLivraison(l));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcule le temps restant en utilisant la formule de Haversine
     */
    private String calculerTempsArrivee(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0 || lon1 == 0) return "Calcul...";

        final int R = 6371; // Rayon de la Terre en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        // On estime la vitesse moyenne d'un livreur à 30 km/h
        double tempsHeures = distance / 30;
        int minutes = (int) (tempsHeures * 60);

        if (minutes < 1) return "Arrivée imminente";
        return minutes + " min";
    }

    private HBox creerCarteLivraison(livraison l) {
        HBox card = new HBox();
        card.setSpacing(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");

        Label iconLabel = new Label("📦");
        iconLabel.setStyle("-fx-font-size: 24px;");

        VBox infoBox = new VBox(5);
        Label dateLabel = new Label(l.getDateLivraison() != null ? l.getDateLivraison().toString().replace("T", " ") : "Date inconnue");
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2D3436;");

        Label adresseLabel = new Label("📍 " + l.getAddressLivraison());
        adresseLabel.setStyle("-fx-text-fill: #636E72; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(dateLabel, adresseLabel);

        // Ajout du temps estimé si la livraison est EN COURS
        String statut = l.getStatutLivraison().toLowerCase();
        if (statut.contains("cours")) {
            String eta = calculerTempsArrivee(l.getCurrentLat(), l.getCurrentLng(), l.getLat(), l.getLng());
            Label etaLabel = new Label("⏳ " + eta);
            etaLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 12px;");
            infoBox.getChildren().add(etaLabel);
        }

        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label statusBadge = new Label(l.getStatutLivraison().toUpperCase());
        styleStatusBadge(statusBadge, l.getStatutLivraison());

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        if (statut.contains("attente") || statut.contains("cours")) {
            Button btnMap = new Button();
            configurerBoutonIcone(btnMap, "/images/map_icon.png", "#E8F5E9", "📍");
            btnMap.setOnAction(event -> openMapWindow(l));
            actionBox.getChildren().add(btnMap);

            if (statut.contains("attente")) {
                Button btnEdit = new Button();
                Button btnDel = new Button();
                configurerBoutonIcone(btnEdit, "/images/edit.png", "#E1F5FE", "✏️");
                configurerBoutonIcone(btnDel, "/images/delete.png", "#FFEBEE", "🗑️");
                btnEdit.setOnAction(event -> ouvrirFormulaire(l, event));
                btnDel.setOnAction(event -> {
                    try { service.deleteOne(l.getId()); chargerDonnees(); } catch (SQLException e) { e.printStackTrace(); }
                });
                actionBox.getChildren().addAll(btnEdit, btnDel);
            }
        } else {
            actionBox.getChildren().add(new Label("✅ Terminé"));
        }

        card.getChildren().addAll(iconLabel, infoBox, statusBadge, actionBox);
        return card;
    }

    public void openMapWindow(livraison l) {
        if (l.getLat() == null || l.getLng() == null) return;
        Stage stage = new Stage();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        stage.setScene(new Scene(webView, 850, 600));
        stage.setTitle("Suivi Temps Réel - Livraison #" + l.getId());
        stage.show();

        URL url = getClass().getResource("/map.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
            webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    // Délai pour s'assurer que Leaflet a fini de charger les tuiles
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(e -> {
                        try {
                            String safeAddress = l.getAddressLivraison().replace("'", "\\'");
                            String temps = calculerTempsArrivee(l.getCurrentLat(), l.getCurrentLng(), l.getLat(), l.getLng());

                            // Coordonnées du livreur (Position destination par défaut si GPS livreur est 0)
                            double livLat = (l.getCurrentLat() != null && l.getCurrentLat() != 0) ? l.getCurrentLat() : l.getLat();
                            double livLng = (l.getCurrentLng() != null && l.getCurrentLng() != 0) ? l.getCurrentLng() : l.getLng();

                            // Appel JavaScript avec les coordonnées de destination, livreur ET le temps estimé
                            String script = String.format(Locale.US, "updateTracking(%f, %f, '%s', %f, %f, '%s')",
                                    l.getLat(), l.getLng(), safeAddress, livLat, livLng, temps);

                            webEngine.executeScript(script);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                    pause.play();
                }
            });
        }
    }

    private void styleStatusBadge(Label badge, String statut) {
        String baseStyle = "-fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;";
        String s = statut.toLowerCase();
        if (s.contains("livré")) {
            badge.setStyle(baseStyle + "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
        } else if (s.contains("attente")) {
            badge.setStyle(baseStyle + "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;");
        } else {
            // Statut EN COURS
            badge.setStyle(baseStyle + "-fx-background-color: #E0E7FF; -fx-text-fill: #3730A3;");
        }
    }

    private void configurerBoutonIcone(Button btn, String path, String color, String text) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8;");
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                ImageView v = new ImageView(new Image(is)); v.setFitHeight(18); v.setFitWidth(18); btn.setGraphic(v);
            } else { btn.setText(text); }
        } catch (Exception e) { btn.setText(text); }
    }

    private void ouvrirFormulaire(livraison l, ActionEvent event) {
        try {
            // Vérification de sécurité pour éviter le NullPointerException
            if (l != null && l.getId() == 0) {
                System.err.println("Erreur : La livraison sélectionnée n'a pas d'ID valide.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterLivraison.fxml"));
            Parent root = loader.load();

            AjouterLivraisoncontrollers c = loader.getController();

            // On ne passe les données que si l'objet est complet
            if (l != null) {
                c.setLivraisonData(l);
            }

            Stage s = (Stage) ((Node) event.getSource()).getScene().getWindow();
            s.setScene(new Scene(root));

        } catch (IOException e) {
            System.err.println("Erreur de chargement du FXML : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML void versAjouter(ActionEvent event) { ouvrirFormulaire(null, event); }
}