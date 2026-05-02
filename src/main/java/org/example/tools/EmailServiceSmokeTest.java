package org.example.tools;

import org.example.models.Article;
import org.example.models.Commande;
import org.example.services.EmailService;

public class EmailServiceSmokeTest {

    public static void main(String[] args) {
        Commande commande = new Commande();
        commande.setNumero("CMD-TEST-EMAIL");
        commande.setAdresseLivraison("Tunis, Tunisie");
        commande.setTotal(750.0);
        commande.setMessagePersonnalise("Chère Issra, merci pour votre commande ! Nous préparons vos créations artisanales avec soin.");
        commande.setAiGenerated(true);

        Article a1 = new Article();
        a1.setTitre("Vase céramique");
        a1.setPrix(500.0);
        commande.addArticle(a1);

        Article a2 = new Article();
        a2.setTitre("Tapis tissé");
        a2.setPrix(250.0);
        commande.addArticle(a2);

        new EmailService().envoyerConfirmationCommande(commande, "client@example.com");
    }
}

