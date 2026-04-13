package org.example.app;

import org.example.models.Produit;
import org.example.services.ServiceProduit;

public class TestCRUDProduit {
    public static void main(String[] args) {
        try {
            ServiceProduit service = new ServiceProduit();

            Produit p = new Produit(
                    "Plastique",
                    "Réutilisable",
                    "Bon",
                    120,
                    "Collecte locale",
                    "Bouteilles recyclables triées et prêtes à réemploi.",
                    "plastique.jpg",
                    65,
                    3,
                    3
            );

            service.ajouter(p);
            System.out.println("Produit inséré avec succès dans la base.");
        } catch (Exception e) {
            System.err.println("Erreur lors du test CRUD Produit : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

