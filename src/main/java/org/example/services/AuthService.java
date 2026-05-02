package org.example.services;

import org.example.models.Role;
import org.example.models.User;
import org.example.utils.MyDatabase;
import org.example.utils.PasswordVerifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    private final Connection connection;

    public AuthService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public User login(String email, String motDePasse) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Veuillez saisir votre email.");
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalArgumentException("Veuillez saisir votre mot de passe.");
        }

        String sql = "SELECT id, nom, prenom, email, motdepasse, role FROM `user` WHERE email = ? AND deleted_at IS NULL LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Email ou mot de passe incorrect.");
                }

                String storedHash = rs.getString("motdepasse");
                if (!PasswordVerifier.verify(motDePasse, storedHash)) {
                    throw new IllegalArgumentException("Email ou mot de passe incorrect.");
                }

                Role role = Role.fromDatabaseValue(rs.getString("role"));
                if (role != Role.ARTISANT && role != Role.CLIENT && role != Role.ADMIN
                        && role != Role.LIVREUR && role != Role.RESPONSABLE) {
                    throw new IllegalArgumentException("Rôle non reconnu : " + rs.getString("role"));
                }

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setRole(role);
                return user;
            }
        }
    }

    /**
     * Connexion via Google — cherche l'utilisateur par email
     * sans vérification de mot de passe (OAuth simulé).
     * Fonctionne pour les comptes avec oauth_provider='google' ET les comptes normaux.
     */
    public User loginAvecGoogle(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;

        // Chercher dans la table user (BD Java piprojet1)
        String sql = "SELECT id, nom, prenom, email, role FROM `user` " +
                     "WHERE email = ? AND deleted_at IS NULL LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Role role = Role.fromDatabaseValue(rs.getString("role"));

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setRole(role);
                return user;
            }
        }
    }
}

