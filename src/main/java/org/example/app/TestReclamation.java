package org.example.app;

import org.example.models.Reclamation;
import org.example.models.ReponseReclamation;
import org.example.models.User;

/**
 * Test des modèles et validations du système de réclamations
 */
public class TestReclamation {

    public static void main(String[] args) {
        System.out.println("===== Tests du Système de Réclamations =====\n");

        // Test 1: Création d'une réclamation valide
        testReclamationValide();

        // Test 2: Validation du titre
        testValidationTitre();

        // Test 3: Validation de la description
        testValidationDescription();

        // Test 4: Validation de l'image
        testValidationImage();

        // Test 5: Création d'une réponse valide
        testReponseValide();

        // Test 6: Validation de la réponse
        testValidationReponse();

        // Test 7: Statuts
        testStatuts();

        System.out.println("\n===== Tous les tests sont terminés =====");
    }

    private static void testReclamationValide() {
        System.out.println("Test 1: Création d'une réclamation valide");
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Problème avec ma commande");
            r.setDescription("J'ai reçu un article endommagé qui ne correspond pas à ma commande");
            r.setImageUrl("https://example.com/image.jpg");
            r.setClientId(1);

            System.out.println("✓ Réclamation créée avec succès");
            System.out.println("  - Titre: " + r.getTitre());
            System.out.println("  - Statut: " + r.getStatut());
            System.out.println("  - Date: " + r.getFormattedDate());
        } catch (Exception e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testValidationTitre() {
        System.out.println("Test 2: Validation du titre");

        // Test titre trop court
        try {
            Reclamation r = new Reclamation();
            r.setTitre("AB");
            System.out.println("✗ Titre trop court devrait être rejeté");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Titre trop court rejeté: " + e.getMessage());
        }

        // Test titre valide
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre valide");
            System.out.println("✓ Titre valide accepté");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }

        // Test titre vide
        try {
            Reclamation r = new Reclamation();
            r.setTitre("");
            System.out.println("✗ Titre vide devrait être rejeté");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Titre vide rejeté: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testValidationDescription() {
        System.out.println("Test 3: Validation de la description");

        // Test description trop courte
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre valide");
            r.setDescription("Court");
            System.out.println("✗ Description trop courte devrait être rejetée");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Description trop courte rejetée: " + e.getMessage());
        }

        // Test description valide
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre valide");
            r.setDescription("Ceci est une description valide");
            System.out.println("✓ Description valide acceptée");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }

        // Test description nulle (optionnelle)
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre valide");
            r.setDescription(null);
            System.out.println("✓ Description nulle acceptée (optionnelle)");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testValidationImage() {
        System.out.println("Test 4: Validation de l'image");

        // Test image valide
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre");
            r.setImageUrl("https://example.com/photo.jpg");
            System.out.println("✓ Image JPG acceptée");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }

        // Test format PNG
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre");
            r.setImageUrl("https://example.com/photo.png");
            System.out.println("✓ Image PNG acceptée");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }

        // Test format invalide
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre");
            r.setImageUrl("https://example.com/document.pdf");
            System.out.println("✗ Format PDF devrait être rejeté");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Format invalide rejeté: " + e.getMessage());
        }

        // Test image nulle (optionnelle)
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Titre");
            r.setImageUrl(null);
            System.out.println("✓ Image nulle acceptée (optionnelle)");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testReponseValide() {
        System.out.println("Test 5: Création d'une réponse valide");
        try {
            ReponseReclamation rep = new ReponseReclamation();
            rep.setContenu("Merci d'avoir signalé ce problème. Nous allons l'examiner de plus près.");
            rep.setReclamationId(1);
            rep.setAdminId(1);

            System.out.println("✓ Réponse créée avec succès");
            System.out.println("  - Contenu: " + rep.getContenu().substring(0, 30) + "...");
            System.out.println("  - Date: " + rep.getFormattedDate());
        } catch (Exception e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testValidationReponse() {
        System.out.println("Test 6: Validation de la réponse");

        // Test contenu trop court
        try {
            ReponseReclamation rep = new ReponseReclamation();
            rep.setContenu("OK");
            System.out.println("✗ Contenu trop court devrait être rejeté");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Contenu trop court rejeté: " + e.getMessage());
        }

        // Test contenu valide
        try {
            ReponseReclamation rep = new ReponseReclamation();
            rep.setContenu("Réponse valide avec suffisamment de caractères");
            System.out.println("✓ Contenu valide accepté");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }

        // Test contenu vide
        try {
            ReponseReclamation rep = new ReponseReclamation();
            rep.setContenu("");
            System.out.println("✗ Contenu vide devrait être rejeté");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Contenu vide rejeté: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testStatuts() {
        System.out.println("Test 7: Statuts");

        String[] statuts = {"en_attente", "en_cours", "resolu", "rejete"};

        for (String statut : statuts) {
            try {
                Reclamation r = new Reclamation();
                r.setTitre("Test statut");
                r.setStatut(statut);
                System.out.println("✓ Statut '" + statut + "' accepté");
            } catch (Exception e) {
                System.out.println("✗ Statut '" + statut + "' rejeté: " + e.getMessage());
            }
        }

        // Test statut invalide
        try {
            Reclamation r = new Reclamation();
            r.setTitre("Test");
            r.setStatut("statut_invalide");
            System.out.println("✓ Statut invalide converti au statut par défaut: " + r.getStatut());
        } catch (Exception e) {
            System.out.println("✗ Erreur: " + e.getMessage());
        }
        System.out.println();
    }
}

