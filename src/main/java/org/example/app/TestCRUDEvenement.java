package org.example.app;

import org.example.models.Evenement;
import org.example.services.ServiceEvenement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestCRUDEvenement {
    public static void main(String[] args) {
        System.out.println("---- TEST DU CRUD EVENEMENT (AVEC VALIDATIONS SYMFONY) ----\n");
        try {
            ServiceEvenement service = new ServiceEvenement();

            // 1. CREATE
            System.out.println("1️⃣ TEST AJOUT");
            Evenement e = new Evenement();
            e.setNom("Festival de la Médina");
            e.setArtisan("Artisan Test");
            e.setDescription("Ce festival est incroyable et magique.");
            e.setDateDebut(LocalDateTime.now().plusDays(5));
            e.setDateFin(LocalDateTime.now().plusDays(7));
            e.setLieu("Tunis - Cité de la Culture");
            e.setCapacite(500);
            e.setTypeArt("Artisanat local");
            e.setTheme("Nocturne");
            e.setPrix(new BigDecimal("25.50"));

            service.ajouter(e);
            System.out.println("✅ Événement ajouté avec succès ! ID attribué : " + e.getId());

            // 2. READ
            System.out.println("\n2️⃣ TEST LECTURE");
            Evenement dbEvent = service.trouverParId(e.getId());
            if (dbEvent != null) {
                System.out.println("✅ Événement récupéré : " + dbEvent.toString());
            }

            // 3. UPDATE
            System.out.println("\n3️⃣ TEST MODIFICATION");
            dbEvent.setCapacite(1000);
            dbEvent.setPrix(new BigDecimal("30.00"));
            service.modifier(dbEvent);
            System.out.println("✅ Événement modifié avec succès (Capacité: 1000, Prix: 30.00).");

            // 4. TEST VALIDATION (Simulation d'Erreur : Date de début dans le passé)
            System.out.println("\n4️⃣ TEST VALIDATION (Doit déclencher une erreur)");
            try {
                Evenement eventInvalide = new Evenement();
                eventInvalide.setNom("Inv"); // Moins de 3 caractères
                eventInvalide.setDescription("Courte"); // Moins de 10 caractères
                eventInvalide.setDateDebut(LocalDateTime.now().minusDays(1)); // Passé
                eventInvalide.setDateFin(LocalDateTime.now());
                eventInvalide.setLieu("T");
                eventInvalide.setCapacite(0); // < 1
                service.ajouter(eventInvalide);
            } catch (IllegalArgumentException ex) {
                System.out.println("✅ L'erreur a bien été bloquée par le service Java (Identique à Symfony !) : "
                        + ex.getMessage());
            }

            // 5. DELETE
            System.out.println("\n5️⃣ TEST SUPPRESSION");
            service.supprimer(e.getId());
            System.out.println("✅ Événement supprimé avec succès.");

        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
