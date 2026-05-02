package org.example.services;

import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gère les réactions ❤️ / 👎 sur les commentaires.
 *
 * Règles identiques aux articles :
 *  – Un utilisateur = un seul vote actif par commentaire.
 *  – Recliquer le même bouton annule le vote.
 *  – Cliquer l'opposé retire l'ancien vote et enregistre le nouveau.
 */
public class ServiceReactionCommentaire {

    public enum TypeReaction { LIKE, DISLIKE }

    private final Connection connection;

    public ServiceReactionCommentaire() {
        this.connection = MyDatabase.getInstance().getConnection();
        try {
            ensureSchema();
        } catch (SQLException e) {
            throw new IllegalStateException(
                "Impossible d'initialiser la table commentaire_reaction : " + e.getMessage(), e);
        }
    }

    /**
     * Enregistre ou annule un vote sur un commentaire.
     *
     * @return "like", "dislike" ou "" (vote annulé)
     */
    public String voter(int commentaireId, int userId, TypeReaction demande) throws SQLException {
        if (commentaireId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }

        String actuelle  = getReactionActuelle(commentaireId, userId);
        String demandeStr = demande == TypeReaction.LIKE ? "like" : "dislike";

        if (demandeStr.equals(actuelle)) {
            // Même bouton → annulation
            supprimerReaction(commentaireId, userId);
            decrementeColonne(commentaireId, demandeStr);
            return "";
        }

        if (!actuelle.isEmpty()) {
            // Changement d'avis → retire l'ancien vote
            decrementeColonne(commentaireId, actuelle);
        }

        if (actuelle.isEmpty()) {
            insererReaction(commentaireId, userId, demandeStr);
        } else {
            mettreAJourReaction(commentaireId, userId, demandeStr);
        }
        incrementeColonne(commentaireId, demandeStr);
        return demandeStr;
    }

    /** Retourne "like", "dislike" ou "" selon le vote actuel de l'utilisateur. */
    public String getReactionActuelle(int commentaireId, int userId) throws SQLException {
        String sql = "SELECT type FROM commentaire_reaction WHERE commentaire_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("type");
            }
        }
        return "";
    }

    // ─── SQL internes ────────────────────────────────────────────────────────

    private void insererReaction(int commentaireId, int userId, String type) throws SQLException {
        String sql = "INSERT INTO commentaire_reaction (commentaire_id, user_id, type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ps.setInt(2, userId);
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    private void mettreAJourReaction(int commentaireId, int userId, String type) throws SQLException {
        String sql = "UPDATE commentaire_reaction SET type = ? WHERE commentaire_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, commentaireId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    private void supprimerReaction(int commentaireId, int userId) throws SQLException {
        String sql = "DELETE FROM commentaire_reaction WHERE commentaire_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void incrementeColonne(int commentaireId, String type) throws SQLException {
        String col = "like".equals(type) ? "likes" : "dislikes";
        String sql = "UPDATE commentaire SET " + col + " = " + col + " + 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ps.executeUpdate();
        }
    }

    private void decrementeColonne(int commentaireId, String type) throws SQLException {
        String col = "like".equals(type) ? "likes" : "dislikes";
        String sql = "UPDATE commentaire SET " + col + " = GREATEST(0, " + col + " - 1) WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ps.executeUpdate();
        }
    }

    // ─── Initialisation du schéma ────────────────────────────────────────────

    private void ensureSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS commentaire_reaction (" +
                "  commentaire_id INT NOT NULL, " +
                "  user_id        INT NOT NULL, " +
                "  type           VARCHAR(8) NOT NULL COMMENT 'like ou dislike', " +
                "  PRIMARY KEY (commentaire_id, user_id), " +
                "  CONSTRAINT fk_creaction_commentaire FOREIGN KEY (commentaire_id) " +
                "    REFERENCES commentaire(id) ON DELETE CASCADE, " +
                "  CONSTRAINT fk_creaction_user FOREIGN KEY (user_id) " +
                "    REFERENCES `user`(id) ON DELETE CASCADE" +
                ")"
            );
        }
    }
}

