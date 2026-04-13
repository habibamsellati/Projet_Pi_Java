package org.example.services;

import org.example.models.User;
import org.example.utils.MyDatabase;
import org.example.utils.PasswordHasher;
import org.example.utils.UserValidation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistance utilisateur. Les relations avec Reclamation, Proposition, etc. sont portées par les FK des autres tables.
 */
public class ServiceUser {

    private final Connection connection;

    public ServiceUser() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    /**
     * Inscription : valide les champs, hash le mot de passe clair.
     */
    public void inscrire(User u, String motDePasseClair) throws SQLException {
        UserValidation.validerNom(u.getNom());
        UserValidation.validerPrenom(u.getPrenom());
        UserValidation.validerEmail(u.getEmail());
        UserValidation.validerMotDePasseClairPourInscription(motDePasseClair);
        UserValidation.validerTelephoneOptionnel(u.getTelephone());
        UserValidation.validerRole(u.getRole());
        UserValidation.validerStatut(u.getStatut());
        UserValidation.validerSexeOptionnel(u.getSexe());

        if (u.getStatut() == null || u.getStatut().isBlank()) {
            u.setStatut(User.STATUT_ACTIF);
        }

        String sql = "INSERT INTO users (nom, prenom, email, mot_de_passe_hash, telephone, role, statut, deleted_at, " +
                "reset_token_hash, reset_token_created_at, reset_token_expires_at, reset_token_request_ip, " +
                "oauth_provider, oauth_provider_id, sexe, avatar, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom().trim());
            ps.setString(2, u.getPrenom().trim());
            ps.setString(3, u.getEmail().trim().toLowerCase());
            ps.setString(4, PasswordHasher.hash(motDePasseClair));
            setStringOrNull(ps, 5, u.getTelephone());
            ps.setString(6, u.getRole().trim());
            ps.setString(7, u.getStatut());
            setTimestampOrNull(ps, 8, null);
            setStringOrNull(ps, 9, u.getResetTokenHash());
            setTimestampOrNull(ps, 10, u.getResetTokenCreatedAt());
            setTimestampOrNull(ps, 11, u.getResetTokenExpiresAt());
            setStringOrNull(ps, 12, u.getResetTokenRequestIp());
            setStringOrNull(ps, 13, u.getOauthProvider());
            setStringOrNull(ps, 14, u.getOauthProviderId());
            setStringOrNull(ps, 15, u.getSexe());
            setStringOrNull(ps, 16, u.getAvatar());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    u.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Suppression douce : enregistre deleted_at = NOW().
     */
    public void softDelete(int userId) throws SQLException {
        String sql = "UPDATE users SET deleted_at = NOW(), statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, User.STATUT_INACTIF);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public User trouverParEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    public boolean authentifier(String email, String motDePasseClair) throws SQLException {
        User u = trouverParEmailActif(email);
        if (u == null) return false;
        return PasswordHasher.verify(motDePasseClair, u.getMotDePasseHash());
    }

    private User trouverParEmailActif(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    public List<User> listerActifs() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE deleted_at IS NULL ORDER BY id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        u.setTelephone(rs.getString("telephone"));
        u.setRole(rs.getString("role"));
        u.setStatut(rs.getString("statut"));
        Timestamp del = rs.getTimestamp("deleted_at");
        if (del != null) u.setDeletedAt(del.toLocalDateTime());
        u.setResetTokenHash(rs.getString("reset_token_hash"));
        Timestamp ts = rs.getTimestamp("reset_token_created_at");
        if (ts != null) u.setResetTokenCreatedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("reset_token_expires_at");
        if (ts != null) u.setResetTokenExpiresAt(ts.toLocalDateTime());
        u.setResetTokenRequestIp(rs.getString("reset_token_request_ip"));
        u.setOauthProvider(rs.getString("oauth_provider"));
        u.setOauthProviderId(rs.getString("oauth_provider_id"));
        u.setSexe(rs.getString("sexe"));
        u.setAvatar(rs.getString("avatar"));
        return u;
    }

    private static void setStringOrNull(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v.trim());
    }

    private static void setTimestampOrNull(PreparedStatement ps, int idx, LocalDateTime v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.TIMESTAMP);
        else ps.setTimestamp(idx, Timestamp.valueOf(v));
    }
}
