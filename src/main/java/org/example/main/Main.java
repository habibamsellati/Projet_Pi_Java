package org.example.main;

import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 1. Instancier le service
        livraisonServices service = new livraisonServices();

        try {
            System.out.println("--- DÉBUT DES TESTS ---");

            // --- TEST 1 : INSERTION ---
            System.out.println("\n1. Test Insertion...");
            livraison nouvelle = new livraison();
            nouvelle.setDateLivraison(LocalDateTime.now());
            nouvelle.setAddressLivraison("Esprit Gazala");
            nouvelle.setStatutLivraison("en_attente");
            nouvelle.setNoteLivreur("123"); // On met un chiffre pour éviter l'erreur de type INT

            service.insertOne(nouvelle);
            System.out.println("=> Insertion réussie !");

            // --- TEST 2 : AFFICHAGE & RÉCUPÉRATION ---
            System.out.println("\n2. Liste des livraisons en base :");
            List<livraison> liste = service.findALL();
            if (liste.isEmpty()) {
                System.out.println("La base est vide.");
                return;
            }

            // On récupère la dernière livraison insérée pour tester l'update et le delete
            livraison livATester = liste.get(liste.size() - 2);
            int testId = livATester.getId();
            System.out.println("=> On va tester l'Update et le Delete sur l'ID : " + testId);



            // --- TEST 3 : MISE À JOUR (UPDATE) ---
            System.out.println("\n3. Test Mise à jour...");
            livATester.setAddressLivraison(" MODIFIÉE");
            livATester.setStatutLivraison("en_cours");

            service.updateOne(livATester);
            System.out.println("=> Mise à jour de l'ID " + testId + " réussie !");

            // --- TEST 4 : SUPPRESSION (DELETE) ---
            System.out.println("\n4. Test Suppression...");
            // Optionnel : décommente la ligne suivante si tu veux vraiment supprimer à chaque test
            // service.deleteOne(testId);
            // System.out.println("=> Suppression de l'ID " + testId + " réussie !");
            System.out.println("=> Suppression ignorée pour garder les données en base.");

            System.out.println("\n--- FIN DES TESTS AVEC SUCCÈS ---");

        } catch (SQLException e) {
            System.err.println("\n❌ ERREUR SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR : " + e.getMessage());
            e.printStackTrace();
        }
    }
}