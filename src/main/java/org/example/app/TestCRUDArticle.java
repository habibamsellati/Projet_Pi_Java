package org.example.app;

import org.example.models.Article;
import org.example.services.ServiceArticle;

import java.sql.SQLException;
import java.util.List;

/**
 * Test CRUD pour les articles
 * Ce fichier permet de tester toutes les opérations CRUD sur les articles
 */
public class TestCRUDArticle {
    
    public static void main(String[] args) {
        ServiceArticle serviceArticle = new ServiceArticle();
        
        System.out.println("========================================");
        System.out.println("   TEST CRUD ARTICLE - Démarrage");
        System.out.println("========================================\n");
        
        try {
            // TEST 1: CRÉER (CREATE)
            System.out.println("📝 TEST 1: Création d'un article...");
            Article nouvelArticle = new Article(
                "Article de Test",
                "Ceci est un contenu de test pour valider le système CRUD des articles.",
                "Artisanat",
                99.99,
                "https://exemple.com/image.jpg",
                3
            );
            
            serviceArticle.ajouter(nouvelArticle);
            System.out.println("✅ Article ajouté avec succès\n");
            
            // TEST 2: LIRE (READ)
            System.out.println("📖 TEST 2: Lecture de tous les articles...");
            List<Article> articles = serviceArticle.afficher();
            System.out.println("✅ Nombre d'articles trouvés: " + articles.size());
            
            if (!articles.isEmpty()) {
                Article premierArticle = articles.get(0);
                System.out.println("\n📌 Premier article:");
                System.out.println("   ID: " + premierArticle.getId());
                System.out.println("   Titre: " + premierArticle.getTitre());
                System.out.println("   Contenu: " + premierArticle.getContenu().substring(0, Math.min(50, premierArticle.getContenu().length())) + "...");
                System.out.println("   Catégorie: " + premierArticle.getCategorie());
                System.out.println("   Prix: " + (premierArticle.getPrix() != null ? premierArticle.getPrix() + "€" : "Non défini"));
                System.out.println("   Date: " + premierArticle.getDatePublication());
                
                // TEST 3: MODIFIER (UPDATE)
                System.out.println("\n✏️ TEST 3: Modification de l'article...");
                premierArticle.setTitre("Article de Test (Modifié)");
                premierArticle.setContenu("Contenu modifié pour tester la mise à jour des articles.");
                premierArticle.setPrix(129.99);
                
                serviceArticle.modifier(premierArticle);
                System.out.println("✅ Article modifié avec succès");
                
                // Vérification de la modification
                articles = serviceArticle.afficher();
                Article articleModifie = articles.stream()
                    .filter(a -> a.getId() == premierArticle.getId())
                    .findFirst()
                    .orElse(null);
                
                if (articleModifie != null) {
                    System.out.println("   Nouveau titre: " + articleModifie.getTitre());
                    System.out.println("   Nouveau prix: " + articleModifie.getPrix() + "€");
                }
                
                // TEST 4: SUPPRIMER (DELETE)
                System.out.println("\n🗑️ TEST 4: Suppression de l'article...");
                System.out.println("   (Article de test ID: " + premierArticle.getId() + ")");
                
                // Décommentez la ligne suivante pour réellement supprimer l'article
                // serviceArticle.supprimer(premierArticle.getId());
                // System.out.println("✅ Article supprimé avec succès");
                
                System.out.println("⚠️ Suppression désactivée par défaut pour préserver les données");
            }
            
            // TEST 5: VALIDATION
            System.out.println("\n🔍 TEST 5: Test des validations...");
            try {
                Article articleInvalide = new Article();
                articleInvalide.setTitre("AB"); // Trop court (min 3 caractères)
                articleInvalide.setContenu("Court"); // Trop court (min 10 caractères)
                serviceArticle.ajouter(articleInvalide);
                System.out.println("❌ ERREUR: L'article invalide n'aurait pas dû être ajouté");
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Validation correcte: " + e.getMessage());
            }
            
            try {
                Article articlePrixNegatif = new Article(
                    "Article Test Prix",
                    "Contenu suffisamment long pour validation",
                    "Artisanat",
                    -50.0, // Prix négatif (invalide)
                    null,
                    3
                );
                serviceArticle.ajouter(articlePrixNegatif);
                System.out.println("❌ ERREUR: L'article avec prix négatif n'aurait pas dû être ajouté");
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Validation correcte: " + e.getMessage());
            }
            
            try {
                Article articleCategorieInvalide = new Article(
                    "Article Test Catégorie",
                    "Contenu suffisamment long pour validation",
                    "CatégorieInexistante", // Catégorie invalide
                    50.0,
                    null,
                    3
                );
                serviceArticle.ajouter(articleCategorieInvalide);
                System.out.println("❌ ERREUR: L'article avec catégorie invalide n'aurait pas dû être ajouté");
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Validation correcte: " + e.getMessage());
            }
            
            System.out.println("\n========================================");
            System.out.println("   TOUS LES TESTS SONT TERMINÉS ✅");
            System.out.println("========================================");
            
        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ ERREUR INATTENDUE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
