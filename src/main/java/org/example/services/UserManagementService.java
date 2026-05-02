package org.example.services;

import org.example.models.Role;
import org.example.models.UserAccount;
import org.example.models.User;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UserManagementService {

    private final Connection connection;

    public UserManagementService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public void inscrireUtilisateur(String nom, String prenom, String email, String motDePasse, Role role) throws SQLException {
        inscrireUtilisateur(nom, prenom, email, motDePasse, role, null, null);
    }

    public void inscrireUtilisateur(String nom, String prenom, String email, String motDePasse, Role role, String sexe, String avatar) throws SQLException {
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Nom et prenom obligatoires.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email obligatoire.");
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalArgumentException("Mot de passe obligatoire.");
        }
        if (emailExiste(email)) {
            throw new IllegalArgumentException("Cet email existe deja.");
        }

        Set<String> columns = getUserColumns();
        String passwordColumn = resolveFirstExisting(columns, "motdepasse", "mot_de_passe_hash", "password");
        if (passwordColumn == null) {
            throw new IllegalStateException("Aucune colonne mot de passe detectee (motdepasse / mot_de_passe_hash / password).");
        }

        String statusColumn = resolveFirstExisting(columns, "statut", "status");
        String createdAtColumn = resolveFirstExisting(columns, "datecreation", "date_creation", "created_at");
        String sexeColumn = resolveFirstExisting(columns, "sexe", "gender");
        String avatarColumn = resolveFirstExisting(columns, "avatar", "avatar_url");

        List<String> insertColumns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        insertColumns.add("nom");
        values.add(nom.trim());
        insertColumns.add("prenom");
        values.add(prenom.trim());
        insertColumns.add("email");
        values.add(email.trim().toLowerCase(Locale.ROOT));
        insertColumns.add(passwordColumn);
        values.add(motDePasse);
        insertColumns.add("role");
        values.add(role == null ? Role.CLIENT.name() : role.name());

        if (sexeColumn != null) {
            insertColumns.add(sexeColumn);
            values.add(sexe == null || sexe.isBlank() ? "Non spécifié" : sexe.trim());
        }
        if (avatarColumn != null) {
            insertColumns.add(avatarColumn);
            values.add(avatar);
        }

        if (statusColumn != null) {
            insertColumns.add(statusColumn);
            values.add("actif");
        }
        if (createdAtColumn != null) {
            insertColumns.add(createdAtColumn);
            values.add(Timestamp.from(Instant.now()));
        }

        String placeholders = String.join(", ", insertColumns.stream().map(c -> "?").toList());
        String sql = "INSERT INTO `user` (" + String.join(", ", insertColumns) + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.executeUpdate();
        }
    }

    public boolean emailExiste(String email) throws SQLException {
        String sql = "SELECT id FROM `user` WHERE LOWER(email)=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User findUserByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return null;
        }

        String sql = "SELECT id, nom, prenom, email, role FROM `user` WHERE LOWER(email)=? LIMIT 1";
        Set<String> columns = getUserColumns();
        String sexeExpr = resolveFirstExisting(columns, "sexe", "gender");
        String avatarExpr = resolveFirstExisting(columns, "avatar", "avatar_url");
        String dynamicSql = "SELECT id, nom, prenom, email, role, "
                + (sexeExpr == null ? "NULL" : sexeExpr) + " AS sexe, "
                + (avatarExpr == null ? "NULL" : avatarExpr) + " AS avatar "
                + "FROM `user` WHERE LOWER(email)=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(dynamicSql)) {
            ps.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public User createGoogleUser(String email, String fullName) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email Google invalide.");
        }

        User existing = findUserByEmail(email);
        if (existing != null) {
            return existing;
        }

        String cleanedEmail = email.trim().toLowerCase(Locale.ROOT);
        String[] parts = splitFullName(fullName);

        Set<String> columns = getUserColumns();
        String passwordColumn = resolveFirstExisting(columns, "motdepasse", "mot_de_passe_hash", "password");
        if (passwordColumn == null) {
            throw new IllegalStateException("Aucune colonne mot de passe detectee pour l'inscription Google.");
        }

        String statusColumn = resolveFirstExisting(columns, "statut", "status");
        String createdAtColumn = resolveFirstExisting(columns, "datecreation", "date_creation", "created_at");
        String avatarColumn = resolveFirstExisting(columns, "avatar", "avatar_url");

        List<String> insertColumns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        insertColumns.add("nom");
        values.add(parts[1]);
        insertColumns.add("prenom");
        values.add(parts[0]);
        insertColumns.add("email");
        values.add(cleanedEmail);
        insertColumns.add(passwordColumn);
        values.add("GOOGLE_AUTH");
        insertColumns.add("role");
        values.add(Role.CLIENT.name());
        String generatedAvatar = new AvatarService().generateAvatarUrl(null, parts[1], parts[0], cleanedEmail);

        String sexeColumn = resolveFirstExisting(columns, "sexe", "gender");
        if (sexeColumn != null) {
            insertColumns.add(sexeColumn);
            values.add("Non spécifié");
        }
        if (avatarColumn != null) {
            insertColumns.add(avatarColumn);
            values.add(generatedAvatar);
        }

        if (statusColumn != null) {
            insertColumns.add(statusColumn);
            values.add("actif");
        }
        if (createdAtColumn != null) {
            insertColumns.add(createdAtColumn);
            values.add(Timestamp.from(Instant.now()));
        }

        String placeholders = String.join(", ", insertColumns.stream().map(c -> "?").toList());
        String sql = "INSERT INTO `user` (" + String.join(", ", insertColumns) + ") VALUES (" + placeholders + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.executeUpdate();
        }

        User created = findUserByEmail(cleanedEmail);
        if (created == null) {
            throw new IllegalStateException("Impossible de relire l'utilisateur Google créé.");
        }
        return created;
    }

    public void updatePasswordByEmail(String email, String newPassword) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email obligatoire.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Nouveau mot de passe obligatoire.");
        }

        Set<String> columns = getUserColumns();
        String passwordColumn = resolveFirstExisting(columns, "motdepasse", "mot_de_passe_hash", "password");
        if (passwordColumn == null) {
            throw new IllegalStateException("Aucune colonne mot de passe detectee.");
        }

        String sql = "UPDATE `user` SET " + passwordColumn + " = ? WHERE LOWER(email) = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email.trim().toLowerCase(Locale.ROOT));
            ps.executeUpdate();
        }
    }

    public List<UserAccount> listerUtilisateurs(String motCle) throws SQLException {
        Set<String> columns = getUserColumns();
        String statusColumn = resolveFirstExisting(columns, "statut", "status");
        String createdAtColumn = resolveFirstExisting(columns, "datecreation", "date_creation", "created_at");
        String deletedAtColumn = resolveFirstExisting(columns, "deleted_at");
        String sexeColumn = resolveFirstExisting(columns, "sexe", "gender");
        String avatarColumn = resolveFirstExisting(columns, "avatar", "avatar_url");

        String statusExpr = statusColumn == null ? "'actif'" : statusColumn;
        String dateExpr = createdAtColumn == null ? "NULL" : createdAtColumn;
        String sexeExpr = sexeColumn == null ? "NULL" : sexeColumn;
        String avatarExpr = avatarColumn == null ? "NULL" : avatarColumn;

        StringBuilder sql = new StringBuilder("SELECT id, nom, prenom, email, role, ")
                .append(statusExpr).append(" AS statut, ")
                .append(dateExpr).append(" AS date_creation, ")
                .append(sexeExpr).append(" AS sexe, ")
                .append(avatarExpr).append(" AS avatar ")
                .append("FROM `user` WHERE 1=1 ");

        if (deletedAtColumn != null) {
            sql.append("AND ").append(deletedAtColumn).append(" IS NULL ");
        }

        boolean hasKeyword = motCle != null && !motCle.isBlank();
        if (hasKeyword) {
            sql.append("AND (LOWER(nom) LIKE ? OR LOWER(prenom) LIKE ? OR LOWER(email) LIKE ? OR LOWER(role) LIKE ?) ");
        }
        sql.append("ORDER BY id DESC");

        List<UserAccount> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            if (hasKeyword) {
                String like = "%" + motCle.trim().toLowerCase(Locale.ROOT) + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserAccount u = new UserAccount();
                    u.setId(rs.getInt("id"));
                    u.setNom(rs.getString("nom"));
                    u.setPrenom(rs.getString("prenom"));
                    u.setEmail(rs.getString("email"));
                    u.setRole(Role.fromDatabaseValue(rs.getString("role")));
                    u.setStatut(rs.getString("statut"));
                    u.setDateCreation(rs.getTimestamp("date_creation"));
                    u.setSexe(rs.getString("sexe"));
                    u.setAvatar(rs.getString("avatar"));
                    users.add(u);
                }
            }
        }
        return users;
    }

    public void desactiverUtilisateur(int userId) throws SQLException {
        Set<String> columns = getUserColumns();
        String deletedAtColumn = resolveFirstExisting(columns, "deleted_at");
        if (deletedAtColumn != null) {
            String sql = "UPDATE `user` SET " + deletedAtColumn + " = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.from(Instant.now()));
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            return;
        }

        String statusColumn = resolveFirstExisting(columns, "statut", "status");
        if (statusColumn != null) {
            String sql = "UPDATE `user` SET " + statusColumn + " = 'inactif' WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            return;
        }

        throw new IllegalStateException("Impossible de desactiver un utilisateur: aucune colonne deleted_at/statut.");
    }

    private Set<String> getUserColumns() throws SQLException {
        Set<String> cols = new HashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        String schema = connection.getCatalog();
        try (ResultSet rs = metaData.getColumns(schema, null, "user", null)) {
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }

        // Certains drivers retournent les colonnes uniquement avec le nom exact de table en backticks.
        if (cols.isEmpty()) {
            try (ResultSet rs = metaData.getColumns(schema, null, "`user`", null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }
        return cols;
    }

    private String resolveFirstExisting(Set<String> cols, String... candidates) {
        for (String candidate : candidates) {
            if (cols.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setRole(Role.fromDatabaseValue(rs.getString("role")));
        try { user.setSexe(rs.getString("sexe")); } catch (SQLException ignored) {}
        try { user.setAvatar(rs.getString("avatar")); } catch (SQLException ignored) {}
        return user;
    }

    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"Utilisateur", "Google"};
        }

        String[] rawParts = fullName.trim().split("\\s+", 2);
        if (rawParts.length == 1) {
            return new String[]{rawParts[0], "Google"};
        }
        return new String[]{rawParts[0], rawParts[1]};
    }
}

