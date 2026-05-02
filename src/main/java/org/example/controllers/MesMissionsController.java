package org.example.controllers;

import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class MesMissionsController {

    private final livraisonServices service = new livraisonServices();

    @FXML
    public void initialize() {
        // Initialisation si nécessaire
    }

    /**
     * Méthode déclenchée par le bouton "Afficher mon planning"
     */
    @FXML
    void afficherPlanning(ActionEvent event) {
        Stage stage = new Stage();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Taille de la fenêtre
        stage.setScene(new Scene(webView, 1100, 800));
        stage.setTitle("AfkArt - Planning Logistique");
        stage.show();

        // 1. Charger le fichier HTML (doit être dans src/main/resources/planning.html)
        URL url = getClass().getResource("/planning.html");

        if (url != null) {
            webEngine.load(url.toExternalForm());

            // 2. On attend que le moteur Web soit prêt avant d'envoyer les données
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    chargerMissionsDansCalendrier(webEngine);
                }
            });
        } else {
            System.err.println("Erreur : Le fichier planning.html est introuvable !");
        }
    }

    /**
     * Récupère les données de la DB et les injecte dans le JavaScript
     */
    private void chargerMissionsDansCalendrier(WebEngine webEngine) {
        try {
            int livreurId = 14; // L'ID qui a Bizerte dans tes screens
            List<livraison> missions = service.findByLivreur(livreurId);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < missions.size(); i++) {
                livraison l = missions.get(i);

                // On s'assure que l'adresse ne contient pas de caractères cassant le JS
                String adresse = l.getAddressLivraison().replace("\"", "\\\"").replace("\n", " ");
                // Format ISO YYYY-MM-DD
                String dateStr = l.getDateLivraison().toLocalDate().toString();

                json.append("{");
                json.append("\"addresslivraison\":\"").append(adresse).append("\",");
                json.append("\"datelivraison\":\"").append(dateStr).append("\",");
                json.append("\"statutlivraison\":\"").append(l.getStatutLivraison()).append("\",");
                json.append("\"lat\":").append(l.getLat() != null ? l.getLat() : "null").append(",");
                json.append("\"lng\":").append(l.getLng() != null ? l.getLng() : "null");
                json.append("}");

                if (i < missions.size() - 1) json.append(",");
            }
            json.append("]");

            String finalJson = json.toString();
            // Injection sécurisée avec des backticks pour le JSON
            webEngine.executeScript("loadMissions(`" + finalJson + "`)");

            System.out.println("Données envoyées : " + finalJson);

        } catch (Exception e) {
            System.err.println("Erreur chargement : " + e.getMessage());
        }
    }
}