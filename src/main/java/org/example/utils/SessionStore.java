package org.example.utils;

import java.sql.*;

/**
 * Simule un stockage de session pour l'utilisateur actuellement connecté.
 * Dynamiquement chargé pour éviter les erreurs de clé étrangère (FK).
 */
public class SessionStore {
    // ID simulé pour l'utilisateur actuel
    public static int currentUserId = -1;

    public static int getUserId() {
        if (currentUserId != -1) return currentUserId;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (Statement st = conn.createStatement()) {
                
                // 1. Détection de la table
                String table = "users";
                try { st.executeQuery("SELECT id FROM users LIMIT 1"); } catch (SQLException e) { table = "user"; }

                // 2. ALIGNEMENT INTELLIGENT : Si des réservations existent, on prend cet ID en priorité
                // Cela permet d'afficher les données que l'utilisateur voit chez l'Artisan
                try (ResultSet rs = st.executeQuery("SELECT user_id FROM reservation WHERE user_id IS NOT NULL LIMIT 1")) {
                    if (rs.next()) {
                        currentUserId = rs.getInt("user_id");
                        System.out.println("DEBUG: Session auto-alignée sur l'ID de réservation: " + currentUserId);
                        return currentUserId;
                    }
                } catch (SQLException e) {}

                // 3. Sinon, chercher Mariem
                String query = "SELECT id FROM " + table + " WHERE (nom LIKE '%mariem%' OR prenom LIKE '%mariem%') LIMIT 1";
                try (ResultSet rs = st.executeQuery(query)) {
                    if (rs.next()) {
                        currentUserId = rs.getInt("id");
                    } else {
                        // Création automatique
                        String insert = "INSERT INTO " + table + " (nom, prenom, email, mot_de_passe_hash, role, statut, created_at) " +
                                       "VALUES ('Mariem', 'Artisane', 'mariem@artisan.tn', 'hashed', 'client', 'actif', NOW())";
                        st.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
                        try (ResultSet keys = st.getGeneratedKeys()) {
                            if (keys.next()) currentUserId = keys.getInt(1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            currentUserId = 1; 
        }
        return currentUserId;
    }
}
