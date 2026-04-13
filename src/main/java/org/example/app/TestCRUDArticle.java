package org.example.app;

import org.example.models.Article;
import org.example.services.ServiceArticle;
import java.sql.SQLException;
import java.util.List;

public class TestCRUDArticle {
    public static void main(String[] args) {
        ServiceArticle sa;
        try {
            sa = new ServiceArticle();
        } catch (RuntimeException e) {
            System.err.println("❌ Connexion base impossible : " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            // 1. Test Ajout
            Article a1 = new Article(
                    "Tapis Berbère",
                    "Fait main en laine naturelle.",
                    250.0,
                    "Textile",
                    "tapis.jpg",
                    1,
                    1
            );
            sa.ajouter(a1);
            System.out.println("✅ Ajout réussi !");

            // 2. Test Affichage
            List<Article> liste = sa.listerCatalogue();
            System.out.println("📋 Liste des articles : " + liste.size());
            for (Article a : liste) {
                System.out.println("- " + a.getTitre() + " (" + a.getCategorie() + ")");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL : " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
