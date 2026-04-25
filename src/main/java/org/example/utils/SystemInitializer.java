package org.example.utils;

import org.example.models.User;
import org.example.services.ServiceUser;
import java.sql.SQLException;

public class SystemInitializer {

    public static void initializeData() {
        ServiceUser su = new ServiceUser();

        System.out.println("--- INITIALISATION DU SYSTÈME (VÉRIFICATION DES COMPTES) ---");

        // 1. Assurer l'Admin
        ensureUser(su, "admin@test.com", "Admin", "PRO", User.ROLE_ADMIN);

        // 2. Assurer l'Artisan
        ensureUser(su, "artisan@test.com", "Artisan", "EXCELLENT", User.ROLE_ARTISANT);

        // 3. Assurer le Client
        ensureUser(su, "client@test.com", "Client", "PREMIUM", User.ROLE_CLIENT);

        System.out.println("--- INITIALISATION TERMINÉE ---");
    }

    private static void ensureUser(ServiceUser su, String email, String nom, String prenom, String role) {
        try {
            User existing = su.trouverParEmail(email);
            if (existing == null) {
                User u = new User();
                u.setEmail(email);
                u.setNom(nom);
                u.setPrenom(prenom);
                u.setRole(role);
                u.setStatut(User.STATUT_ACTIF);
                u.setTelephone("22334455");
                u.setSexe(User.SEXE_HOMME);

                su.inscrire(u, "AfkArt@2026"); // MDP conforme: Majuscule, Chiffre, Symbole
                System.out.println("[CREATION] Compte créé : " + email + " (Role: " + role + ")");
            } else {
                // S'il existe mais n'a pas le bon rôle (cas de confusion précédente)
                if (!role.equals(existing.getRole())) {
                    System.out.println("[UPDATE] Mise à jour du rôle pour : " + email + " -> " + role);
                    // On pourrait appeler une méthode d'update de rôle si besoin
                }
                System.out.println("[OK] Compte déjà présent : " + email);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de " + email + " : " + e.getMessage());
        }
    }
}
