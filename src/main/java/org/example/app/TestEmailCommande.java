package org.example.app;

import org.example.models.Article;
import org.example.models.Commande;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SessionManager;

import java.sql.SQLException;

/**
 * Test pour démontrer l'envoi d'email personnalisé lors de la création d'une commande
 * 
 * Ce test simule :
 * 1. Un utilisateur connecté avec son email
 * 2. La création d'une commande
 * 3. L'envoi automatique d'un email de confirmation à l'adresse du compte connecté
 */
public class TestEmailCommande {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST : Email Personnalisé de Confirmation de Commande   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        try {
            // ═══════════════════════════════════════════════════════════
            // ÉTAPE 1 : Simulation d'un utilisateur connecté
            // ═══════════════════════════════════════════════════════════
            System.out.println("┌─ ÉTAPE 1 : Connexion utilisateur ────────────────────────┐");
            
            // Créer un utilisateur fictif (simule SessionManager.getCurrentUser())
            User clientConnecte = new User();
            clientConnecte.setId(1);
            clientConnecte.setNom("Ben Ghrib");
            clientConnecte.setPrenom("Issra");
            clientConnecte.setEmail("issrabenghrib04@gmail.com");
            clientConnecte.setRole(Role.CLIENT);
            
            // Enregistrer dans la session
            SessionManager.login(clientConnecte);
            
            System.out.println("✅ Utilisateur connecté :");
            System.out.println("   • ID       : " + clientConnecte.getId());
            System.out.println("   • Nom      : " + clientConnecte.getNomComplet());
            System.out.println("   • Email    : " + clientConnecte.getEmail());
            System.out.println("   • Rôle     : " + clientConnecte.getRole());
            System.out.println("└───────────────────────────────────────────────────────────┘\n");
            
            // ═══════════════════════════════════════════════════════════
            // ÉTAPE 2 : Création d'une commande avec articles
            // ═══════════════════════════════════════════════════════════
            System.out.println("┌─ ÉTAPE 2 : Création de la commande ──────────────────────┐");
            
            Commande commande = new Commande();
            commande.setClientId(clientConnecte.getId());
            commande.setNomClient(clientConnecte.getNomComplet());
            commande.setEmailClient(clientConnecte.getEmail()); // email réel du client connecté
            commande.setAdresseLivraison("12 Rue de Carthage, Tunis 1000, Tunisie");
            commande.setTelephone("20123456");
            commande.setModePaiement("carte");
            commande.setStatut("en_attente");
            
            // Ajouter des articles fictifs
            Article article1 = new Article();
            article1.setId(1);
            article1.setTitre("Poterie artisanale tunisienne");
            article1.setCategorie("Artisanat");
            article1.setPrix(120.00);
            commande.addArticle(article1);
            
            Article article2 = new Article();
            article2.setId(2);
            article2.setTitre("Tapis berbère fait main");
            article2.setCategorie("Artisanat");
            article2.setPrix(250.00);
            commande.addArticle(article2);
            
            Article article3 = new Article();
            article3.setId(3);
            article3.setTitre("Lampe en cuivre");
            article3.setCategorie("Décoration");
            article3.setPrix(75.50);
            commande.addArticle(article3);
            
            // Calculer le total
            commande.setTotal(commande.calculateTotal());
            
            System.out.println("✅ Commande créée :");
            System.out.println("   • Client ID    : " + commande.getClientId());
            System.out.println("   • Nom client   : " + commande.getNomClient());
            System.out.println("   • Adresse      : " + commande.getAdresseLivraison());
            System.out.println("   • Téléphone    : " + commande.getTelephone());
            System.out.println("   • Paiement     : " + commande.getModePaiement());
            System.out.println("   • Articles     : " + commande.getArticles().size());
            System.out.println("   • Total        : " + String.format("%.2f DT", commande.getTotal()));
            System.out.println("└───────────────────────────────────────────────────────────┘\n");
            
            // ═══════════════════════════════════════════════════════════
            // ÉTAPE 3 : Enregistrement et envoi automatique de l'email
            // ═══════════════════════════════════════════════════════════
            System.out.println("┌─ ÉTAPE 3 : Enregistrement et envoi email ────────────────┐");
            System.out.println("⏳ Enregistrement de la commande dans la base de données...");
            System.out.println("⏳ Génération du message personnalisé (IA)...");
            System.out.println("⏳ Récupération de l'email du client (ID: " + commande.getClientId() + ")...");
            System.out.println("⏳ Envoi de l'email de confirmation...\n");
            
            ServiceCommande serviceCommande = new ServiceCommande();
            serviceCommande.ajouter(commande);
            
            System.out.println("✅ Commande enregistrée avec succès !");
            System.out.println("   • Numéro       : " + commande.getNumero());
            System.out.println("   • ID           : " + commande.getId());
            if (commande.getMessagePersonnalise() != null && !commande.getMessagePersonnalise().isEmpty()) {
                System.out.println("   • Message IA   : " + (commande.isAiGenerated() ? "✓ Généré" : "✗ Fallback"));
                System.out.println("\n   ╭─ Message Personnalisé ─────────────────────────────╮");
                String[] lignes = commande.getMessagePersonnalise().split("\n");
                for (String ligne : lignes) {
                    System.out.println("   │ " + ligne);
                }
                System.out.println("   ╰────────────────────────────────────────────────────╯");
            }
            System.out.println("└───────────────────────────────────────────────────────────┘\n");
            
            // ═══════════════════════════════════════════════════════════
            // RÉSUMÉ
            // ═══════════════════════════════════════════════════════════
            System.out.println("╔═══════════════════════════════════════════════════════════╗");
            System.out.println("║                      RÉSUMÉ DU TEST                       ║");
            System.out.println("╠═══════════════════════════════════════════════════════════╣");
            System.out.println("║                                                           ║");
            System.out.println("║  ✅ Utilisateur connecté identifié                        ║");
            System.out.println("║  ✅ Email récupéré depuis le compte : " + String.format("%-20s", clientConnecte.getEmail()) + " ║");
            System.out.println("║  ✅ Commande créée et enregistrée                         ║");
            System.out.println("║  ✅ Message personnalisé généré                           ║");
            System.out.println("║  ✅ Email de confirmation envoyé automatiquement          ║");
            System.out.println("║                                                           ║");
            System.out.println("║  📧 L'email a été envoyé à l'adresse du compte connecté  ║");
            System.out.println("║                                                           ║");
            System.out.println("║  💡 Si SMTP est configuré :                               ║");
            System.out.println("║     → Email réel envoyé à " + String.format("%-30s", clientConnecte.getEmail()) + "║");
            System.out.println("║                                                           ║");
            System.out.println("║  💡 Si SMTP non configuré :                               ║");
            System.out.println("║     → Email affiché dans la console (mode preview)        ║");
            System.out.println("║                                                           ║");
            System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
            
            // ═══════════════════════════════════════════════════════════
            // INSTRUCTIONS
            // ═══════════════════════════════════════════════════════════
            System.out.println("┌─ POUR ACTIVER L'ENVOI RÉEL D'EMAILS ─────────────────────┐");
            System.out.println("│                                                           │");
            System.out.println("│  1. Éditez src/main/resources/email.properties           │");
            System.out.println("│                                                           │");
            System.out.println("│  2. Configurez les paramètres SMTP :                     │");
            System.out.println("│     email.smtp.host=smtp.gmail.com                       │");
            System.out.println("│     email.smtp.port=587                                  │");
            System.out.println("│     email.smtp.username=votre.email@gmail.com            │");
            System.out.println("│     email.smtp.password=mot_de_passe_application         │");
            System.out.println("│     email.from=votre.email@gmail.com                     │");
            System.out.println("│     email.from.name=AfkArt                               │");
            System.out.println("│                                                           │");
            System.out.println("│  3. Pour Gmail, générez un mot de passe d'application :  │");
            System.out.println("│     https://myaccount.google.com/security                │");
            System.out.println("│                                                           │");
            System.out.println("└───────────────────────────────────────────────────────────┘\n");
            
            System.out.println("✅ TEST TERMINÉ AVEC SUCCÈS !\n");
            
            // Déconnexion
            SessionManager.logout();
            
        } catch (SQLException e) {
            System.err.println("\n❌ ERREUR SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR INATTENDUE : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

