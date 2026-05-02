package org.example.services;

import org.example.models.User;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BadWordRepository {

    private final Connection connection;

    public BadWordRepository() {
        this.connection = MyDatabase.getInstance().getConnection();
        try {
            ensureSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser les tables bad_words: " + e.getMessage(), e);
        }
    }

    public List<String> findAllWords() throws SQLException {
        List<String> words = new ArrayList<>();
        String sql = "SELECT mot FROM bad_words ORDER BY mot ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                words.add(rs.getString("mot"));
            }
        }

        return words;
    }

    public boolean addWord(String word, String langue, String gravite, User addedBy) throws SQLException {
        String sql = "INSERT INTO bad_words (mot, langue, gravite, ajoute_par, date_ajout) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, word);
            ps.setString(2, langue == null || langue.isBlank() ? "fr" : langue.trim().toLowerCase());
            ps.setString(3, gravite == null || gravite.isBlank() ? "MOYEN" : gravite.trim().toUpperCase());
            ps.setString(4, addedBy == null ? "system" : addedBy.getNomComplet());
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteWord(String word) throws SQLException {
        String sql = "DELETE FROM bad_words WHERE mot = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, word);
            return ps.executeUpdate() > 0;
        }
    }

    public void saveFlag(int commentaireId, int userId, String detectedWordsCsv) throws SQLException {
        String sql = "INSERT INTO commentaire_moderation_flag (commentaire_id, user_id, mots_detectes, date_flag) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ps.setInt(2, userId);
            ps.setString(3, detectedWordsCsv);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    private void ensureSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS bad_words (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "mot VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE, " +
                    "langue VARCHAR(10) NOT NULL DEFAULT 'fr', " +
                    "gravite VARCHAR(10) NOT NULL DEFAULT 'MOYEN', " +
                    "ajoute_par VARCHAR(120) NULL, " +
                    "date_ajout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            st.execute("CREATE TABLE IF NOT EXISTS commentaire_moderation_flag (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "commentaire_id INT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "mots_detectes VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL, " +
                    "date_flag DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_flag_commentaire FOREIGN KEY (commentaire_id) REFERENCES commentaire(id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_flag_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE" +
                    ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            st.execute("INSERT IGNORE INTO bad_words (mot, langue, gravite, ajoute_par) VALUES " +
                    "('idiot', 'fr', 'MOYEN', 'system')," +
                    "('nul', 'fr', 'LEGER', 'system')," +
                    "('stupide', 'fr', 'MOYEN', 'system')," +
                    "('horrible', 'fr', 'MOYEN', 'system')," +
                    "('mauvais', 'fr', 'LEGER', 'system')");
        }
    }
}


