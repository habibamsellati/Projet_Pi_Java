package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charge ta page principale (Assure-toi que le chemin est correct)
        Parent root = FXMLLoader.load(getClass().getResource("/AfficherLivraison.fxml"));
        primaryStage.setTitle("Gestion des Livraisons - AfkArt");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        // --- CONFIGURATION RENDU CARTE (CRUCIAL) ---
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.vsync", "false");

        // 1. Instancier le service
        livraisonServices service = new livraisonServices();

        try {
            System.out.println("--- DÉBUT DES TESTS CONSOLE ---");

            // --- TEST 1 : INSERTION ---
            System.out.println("1. Test Insertion...");
            livraison nouvelle = new livraison();
            nouvelle.setDateLivraison(LocalDateTime.now());
            nouvelle.setAddressLivraison("Esprit Gazala");
            nouvelle.setStatutLivraison("en_attente");
            nouvelle.setNoteLivreur("123");
            // On ajoute des coordonnées de test pour Ariana (Tunisie) pour la map
            nouvelle.setLat(36.8665);
            nouvelle.setLng(10.1647);

            service.insertOne(nouvelle);
            System.out.println("=> Insertion réussie !");

            // --- TEST 2 : LISTE ---
            List<livraison> liste = service.findALL();
            System.out.println("Nombre de livraisons en base : " + liste.size());

            System.out.println("--- FIN DES TESTS : LANCEMENT DE L'INTERFACE ---");

        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL : " + e.getMessage());
        }

        // 2. Lancer l'application JavaFX
        launch(args);
    }
}