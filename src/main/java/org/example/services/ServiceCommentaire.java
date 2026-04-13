package org.example.services;

import org.example.models.Commentaire;
import org.example.utils.JdbcUtil;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD commentaires — schéma V1 (date_pub, likes/dislikes, parent) avec repli sur l’ancien (date_publication, note).
 */
public class ServiceCommentaire {
    private final Connection connection;

    public ServiceCommentaire() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    public void ajouter(Commentaire c) throws SQLException {
        valider(c);
        int uid = c.getUserId() == 0 ? 3 : c.getUserId();

        String sqlV1 = "INSERT INTO commentaire (article_id, user_id, parent_id, contenu, date_pub, likes, dislikes) VALUES (?,?,?,?,NOW(),0,0)";
        try (PreparedStatement ps = connection.prepareStatement(sqlV1, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getArticleId());
            ps.setInt(2, uid);
            if (c.getParentId() != null) {
                ps.setInt(3, c.getParentId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, c.getContenu().trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
            return;
        } catch (SQLException ex) {
            if (!JdbcUtil.isMissingColumnError(ex)) {
                throw ex;
            }
            // Ancien schéma avec note
            String sqlLegacy = "INSERT INTO commentaire (article_id, user_id, contenu, date_publication, note) VALUES (?,?,?,?,NOW(),?)";
            try (PreparedStatement ps = connection.prepareStatement(sqlLegacy, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, c.getArticleId());
                ps.setInt(2, uid);
                ps.setString(3, c.getContenu().trim());
                int note = (c.getNote() != null) ? c.getNote() : 3;
                if (note < 1) note = 1;
                if (note > 5) note = 5;
                ps.setInt(4, note);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) c.setId(keys.getInt(1));
                }
            }
        }
    }

    public void supprimer(int commentaireId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM commentaire WHERE parent_id = ?")) {
            ps.setInt(1, commentaireId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM commentaire WHERE id = ?")) {
            ps.setInt(1, commentaireId);
            ps.executeUpdate();
        }
    }

    public List<Commentaire> afficherParArticle(int articleId) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String orderV1 = "SELECT * FROM commentaire WHERE article_id = ? ORDER BY date_pub DESC";
        try (PreparedStatement ps = connection.prepareStatement(orderV1)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            if (!JdbcUtil.isMissingColumnError(e)) {
                throw e;
            }
            String orderLegacy = "SELECT * FROM commentaire WHERE article_id = ? ORDER BY date_publication DESC";
            try (PreparedStatement ps = connection.prepareStatement(orderLegacy)) {
                ps.setInt(1, articleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Commentaire map(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();
        c.setId(rs.getInt("id"));
        c.setArticleId(rs.getInt("article_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setContenu(rs.getString("contenu"));
        if (JdbcUtil.hasColumn(rs, "parent_id")) {
            int p = rs.getInt("parent_id");
            c.setParentId(rs.wasNull() ? null : p);
        }
        Timestamp ts = null;
        if (JdbcUtil.hasColumn(rs, "date_pub")) {
            ts = rs.getTimestamp("date_pub");
        }
        if (ts == null && JdbcUtil.hasColumn(rs, "date_publication")) {
            ts = rs.getTimestamp("date_publication");
        }
        if (ts != null) {
            c.setDatePub(ts.toLocalDateTime());
        }
        if (JdbcUtil.hasColumn(rs, "likes")) {
            c.setLikes(rs.getInt("likes"));
        }
        if (JdbcUtil.hasColumn(rs, "dislikes")) {
            c.setDislikes(rs.getInt("dislikes"));
        }
        if (JdbcUtil.hasColumn(rs, "note")) {
            int n = rs.getInt("note");
            if (!rs.wasNull()) c.setNote(n);
        }
        return c;
    }

    private void valider(Commentaire c) {
        String t = c.getContenu() == null ? "" : c.getContenu().trim();
        if (t.length() < 5 || t.length() > 255) {
            throw new IllegalArgumentException("Le commentaire doit contenir entre 5 et 255 caractères.");
        }
    }
}
