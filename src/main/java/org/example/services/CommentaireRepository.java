package org.example.services;

import org.example.models.Article;
import org.example.models.Commentaire;
import org.example.models.Role;
import org.example.models.User;
import org.example.utils.MyDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CommentaireRepository {
    private static final String FALLBACK_SCHEMA = "CREATE TABLE IF NOT EXISTS commentaire (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "contenu VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL, " +
            "datepub DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "likes INT NOT NULL DEFAULT 0, " +
            "dislikes INT NOT NULL DEFAULT 0, " +
            "article_id INT NOT NULL, " +
            "user_id INT NOT NULL, " +
            "parent_id INT NULL, " +
            "CONSTRAINT fk_commentaire_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE, " +
            "CONSTRAINT fk_commentaire_auteur FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE, " +
            "CONSTRAINT fk_commentaire_parent FOREIGN KEY (parent_id) REFERENCES commentaire(id) ON DELETE CASCADE" +
            ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

    private final Connection connection;

    public CommentaireRepository() {
        this.connection = MyDatabase.getInstance().getConnection();
        try {
            ensureSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser la table commentaire : " + e.getMessage(), e);
        }
    }

    public void save(Commentaire commentaire) throws SQLException {
        String sql = "INSERT INTO commentaire (contenu, datepub, likes, dislikes, article_id, user_id, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, commentaire.getContenu());
            ps.setTimestamp(2, commentaire.getDatePub() == null ? new Timestamp(System.currentTimeMillis()) : commentaire.getDatePub());
            ps.setInt(3, commentaire.getLikes());
            ps.setInt(4, commentaire.getDislikes());
            ps.setInt(5, commentaire.getArticle().getId());
            ps.setInt(6, commentaire.getAuteur().getId());
            if (commentaire.getParent() == null || commentaire.getParent().getId() <= 0) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, commentaire.getParent().getId());
            }

            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    commentaire.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Commentaire> findByArticle(Article article) throws SQLException {
        if (article == null || article.getId() <= 0) {
            return new ArrayList<>();
        }

        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.id, c.contenu, c.datepub, c.likes, c.dislikes, c.parent_id, " +
                "u.id AS auteur_id, u.nom, u.prenom, u.email, u.role " +
                "FROM commentaire c " +
                "LEFT JOIN `user` u ON c.user_id = u.id " +
                "WHERE c.article_id = ? " +
                "ORDER BY c.datepub DESC, c.id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, article.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    commentaires.add(mapRow(rs, article));
                }
            }
        }

        return commentaires;
    }

    public void updateContenu(int commentaireId, String contenu) throws SQLException {
        String sql = "UPDATE commentaire SET contenu = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, contenu);
            ps.setInt(2, commentaireId);
            ps.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM commentaire WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Commentaire mapRow(ResultSet rs, Article article) throws SQLException {
        Commentaire commentaire = new Commentaire();
        commentaire.setId(rs.getInt("id"));
        commentaire.setContenu(rs.getString("contenu"));
        commentaire.setDatePub(rs.getTimestamp("datepub"));
        commentaire.setLikes(rs.getInt("likes"));
        commentaire.setDislikes(rs.getInt("dislikes"));
        commentaire.setArticle(article);

        User auteur = new User();
        auteur.setId(rs.getInt("auteur_id"));
        auteur.setNom(rs.getString("nom"));
        auteur.setPrenom(rs.getString("prenom"));
        auteur.setEmail(rs.getString("email"));
        auteur.setRole(Role.fromDatabaseValue(rs.getString("role")));
        commentaire.setAuteur(auteur);

        int parentId = rs.getInt("parent_id");
        if (!rs.wasNull()) {
            Commentaire parent = new Commentaire();
            parent.setId(parentId);
            commentaire.setParent(parent);
        }

        return commentaire;
    }

    private void ensureSchema() throws SQLException {
        String sql = loadSchemaSql();
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
            st.execute("ALTER TABLE commentaire CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            st.execute("ALTER TABLE commentaire MODIFY contenu VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL");
        }
    }

    private String loadSchemaSql() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sql/commentaire.sql")) {
            if (inputStream == null) {
                return FALLBACK_SCHEMA;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return FALLBACK_SCHEMA;
        }
    }
}

