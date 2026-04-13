package org.example.app;

import org.example.models.Proposition;
import org.example.services.ServiceProposition;

public class TestCRUDProposition {
    public static void main(String[] args) {
        try {
            ServiceProposition service = new ServiceProposition();

            Proposition p = new Proposition(
                    1, // produit_id
                    3, // user_id
                    "Offre client",
                    "Je propose ce prix car je peux récupérer rapidement.",
                    30.0,
                    "22111222",
                    "offre.jpg",
                    ServiceProposition.STATUT_EN_ATTENTE
            );

            service.ajouter(p);
            System.out.println("Proposition ajoutée avec succès.");
        } catch (Exception e) {
            System.err.println("Erreur TestCRUDProposition: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

