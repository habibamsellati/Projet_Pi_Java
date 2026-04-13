package org.example.services;

import org.example.models.Article;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD Articles (e-commerce V1) — validations + suppression bloquée si commandes liées.
 */
public class ServiceArticle {
    private final Connection connection;

    public ServiceArticle() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    public void ajouter(Article a) throws SQLException {
        valider(a);
        int artisan = (a.getArtisanId() == 0) ? 3 : a.getArtisanId();
        int user = (a.getUserId() == 0) ? artisan : a.getUserId();
        String sql = "INSERT INTO article (titre, contenu, date_publication, prix, categorie, image, artisan_id, user_id) VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getTitre().trim());
            ps.setString(2, a.getContenu().trim());
            ps.setDouble(3, a.getPrix());
            ps.setString(4, emptyToNull(a.getCategorie()));
            ps.setString(5, emptyToNull(a.getImage() != null ? a.getImage() : a.getImageUrl()));
            ps.setInt(6, artisan);
            ps.setInt(7, user);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setId(keys.getInt(1));
            }
        }
    }

    /**
     * Met à jour uniquement si l'article appartient à l'artisan (user_id ou artisan_id).
     */
    public void modifier(Article a, int artisanUserId) throws SQLException {
        valider(a);
        if (a.getId() <= 0) throw new IllegalArgumentException("ID article invalide.");
        String sql = "UPDATE article SET titre=?, contenu=?, prix=?, categorie=?, image=? WHERE id=? AND (artisan_id=? OR user_id=?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getTitre().trim());
            ps.setString(2, a.getContenu().trim());
            ps.setDouble(3, a.getPrix());
            ps.setString(4, emptyToNull(a.getCategorie()));
            ps.setString(5, emptyToNull(a.getImage() != null ? a.getImage() : a.getImageUrl()));
            ps.setInt(6, a.getId());
            ps.setInt(7, artisanUserId);
            ps.setInt(8, artisanUserId);
            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("Article introuvable ou vous n'êtes pas l'auteur.");
        }
    }

    /**
     * Supprime l'article et les commentaires (CASCADE ou manuel). Bloqué si l'article figure dans des commandes.
     */
    public void supprimer(int articleId) throws SQLException {
        if (countLignesCommandeArticle(articleId) > 0) {
            throw new IllegalStateException(
                    "Impossible de supprimer : cet article est présent dans une ou plusieurs commandes. Retirez-le des commandes ou annulez-les d'abord."
            );
        }
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM commentaire WHERE article_id = ?")) {
                ps.setInt(1, articleId);
                ps.executeUpdate();
            } catch (SQLException ignored) {
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM article WHERE id = ?")) {
                ps.setInt(1, articleId);
                int n = ps.executeUpdate();
                if (n == 0) throw new IllegalArgumentException("Article introuvable.");
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private int countLignesCommandeArticle(int articleId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM ligne_commande_article WHERE article_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) {
            try {
                String sqlAlt = "SELECT COUNT(*) AS c FROM ligne_commande WHERE article_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sqlAlt)) {
                    ps.setInt(1, articleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getInt("c");
                    }
                }
            } catch (SQLException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public List<Article> listerCatalogue() throws SQLException {
        return lister("ORDER BY date_publication DESC");
    }

    public Article trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM article WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    private List<Article> lister(String orderBy) throws SQLException {
        try {
            return listerQuery("SELECT * FROM article " + orderBy);
        } catch (SQLException e) {
            return listerQuery("SELECT * FROM article ORDER BY id DESC");
        }
    }

    private List<Article> listerQuery(String sql) throws SQLException {
        List<Article> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Article map(ResultSet rs) throws SQLException {
        Article a = new Article();
        a.setId(rs.getInt("id"));
        a.setTitre(rs.getString("titre"));
        a.setContenu(rs.getString("contenu"));
        Timestamp ts = rs.getTimestamp("date_publication");
        if (ts == null) ts = rs.getTimestamp("date");
        if (ts != null) a.setDatePublication(ts.toLocalDateTime());
        a.setPrix(rs.getDouble("prix"));
        a.setCategorie(rs.getString("categorie"));
        a.setImage(rs.getString("image"));
        a.setArtisanId(rs.getInt("artisan_id"));
        try {
            a.setUserId(rs.getInt("user_id"));
        } catch (SQLException ignored) {
            a.setUserId(a.getArtisanId());
        }
        return a;
    }

    private void valider(Article a) {
        String t = a.getTitre() == null ? "" : a.getTitre().trim();
        if (t.length() < 3 || t.length() > 255) {
            throw new IllegalArgumentException("Le titre doit contenir entre 3 et 255 caractères.");
        }
        String c = a.getContenu() == null ? "" : a.getContenu().trim();
        if (c.length() < 10) {
            throw new IllegalArgumentException("Le contenu doit contenir au moins 10 caractères.");
        }
        if (a.getPrix() < 0) {
            throw new IllegalArgumentException("Le prix doit être positif ou nul.");
        }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
