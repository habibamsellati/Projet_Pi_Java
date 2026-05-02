package org.example.services;

import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gère les réactions like/dislike sur les articles.
 *
 * Règles :
 *  – Un utilisateur ne peut avoir qu'un seul vote actif à la fois par article.
 *  – Cliquer une deuxième fois sur le même bouton annule le vote.
 *  – Cliquer sur le bouton opposé retire l'ancien vote et enregistre le nouveau.
 *
 * Résultat retourné par voter() :
 *  "like"    → l'utilisateur like maintenant
 *  "dislike" → l'utilisateur dislike maintenant
 *  ""        → le vote a été annulé (toggle)
 */
public class ServiceReactionArticle {

    public enum TypeReaction { LIKE, DISLIKE }

    private final Connection connection;

    public ServiceReactionArticle() {
        this.connection = MyDatabase.getInstance().getConnection();
        try {
            ensureSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser les tables de réactions : " + e.getMessage(), e);
        }
    }

    /**
     * Enregistre ou annule un vote.
     *
     * @return la réaction active après l'opération ("like", "dislike" ou "" si annulée)
     */
    public String voter(int articleId, int userId, TypeReaction demande) throws SQLException {
        if (articleId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }

        String actuelle = getReactionActuelle(articleId, userId);
        String demandeStr = demande == TypeReaction.LIKE ? "like" : "dislike";

        if (demandeStr.equals(actuelle)) {
            // Même bouton cliqué une deuxième fois → annulation
            supprimerReaction(articleId, userId);
            decrementeColonne(articleId, demandeStr);
            return "";
        }

        if (!actuelle.isEmpty()) {
            // Changement d'avis → on retire l'ancien vote
            decrementeColonne(articleId, actuelle);
        }

        if (actuelle.isEmpty()) {
            insererReaction(articleId, userId, demandeStr);
        } else {
            mettreAJourReaction(articleId, userId, demandeStr);
        }
        incrementeColonne(articleId, demandeStr);
        return demandeStr;
    }

    /**
     * Retourne la réaction actuelle de l'utilisateur : "like", "dislike" ou "".
     */
    public String getReactionActuelle(int articleId, int userId) throws SQLException {
        String sql = "SELECT type FROM article_reaction WHERE article_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("type");
                }
            }
        }
        return "";
    }

    // ─── SQL internes ────────────────────────────────────────────────────────

    private void insererReaction(int articleId, int userId, String type) throws SQLException {
        String sql = "INSERT INTO article_reaction (article_id, user_id, type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ps.setInt(2, userId);
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    private void mettreAJourReaction(int articleId, int userId, String type) throws SQLException {
        String sql = "UPDATE article_reaction SET type = ? WHERE article_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, articleId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    private void supprimerReaction(int articleId, int userId) throws SQLException {
        String sql = "DELETE FROM article_reaction WHERE article_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void incrementeColonne(int articleId, String type) throws SQLException {
        String col = "like".equals(type) ? "likes" : "dislikes";
        String sql = "UPDATE article SET " + col + " = " + col + " + 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ps.executeUpdate();
        }
    }

    private void decrementeColonne(int articleId, String type) throws SQLException {
        String col = "like".equals(type) ? "likes" : "dislikes";
        // On s'assure de ne jamais descendre en dessous de 0
        String sql = "UPDATE article SET " + col + " = GREATEST(0, " + col + " - 1) WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ps.executeUpdate();
        }
    }

    // ─── Initialisation du schéma ────────────────────────────────────────────

    private void ensureSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // Ajoute la colonne dislikes si elle n'existe pas encore dans article
            try {
                st.executeUpdate("ALTER TABLE article ADD COLUMN dislikes INT NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // La colonne existe déjà — on ignore
            }

            // Crée la table de tracking des votes
            st.execute(
                "CREATE TABLE IF NOT EXISTS article_reaction (" +
                "  article_id INT NOT NULL, " +
                "  user_id    INT NOT NULL, " +
                "  type       VARCHAR(8) NOT NULL COMMENT 'like ou dislike', " +
                "  PRIMARY KEY (article_id, user_id), " +
                "  CONSTRAINT fk_reaction_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE, " +
                "  CONSTRAINT fk_reaction_user    FOREIGN KEY (user_id)    REFERENCES `user`(id)  ON DELETE CASCADE" +
                ")"
            );
        }
    }
}

