package org.example.services;

import org.example.models.Reclamation;
import org.example.models.ReponseReclamation;
import org.example.models.User;
import org.example.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiceReclamation {
    private Connection connection;
    private final BadWordService badWordService;

    public ServiceReclamation() {
        connection = MyDatabase.getInstance().getConnection();
        badWordService = new BadWordService();
        ensureSchemaCompatibility();
    }

    private void ensureSchemaCompatibility() {
        if (connection == null) {
            return;
        }

        try {
            if (!columnExists("reclamation", "description")) {
                if (columnExists("reclamation", "descripition")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN descripition description TEXT NULL");
                    }
                } else {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation ADD COLUMN description TEXT NULL AFTER titre");
                    }
                }
            }

            if (!columnExists("reclamation", "date_creation")) {
                if (columnExists("reclamation", "date _creation")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN `date _creation` date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                } else if (columnExists("reclamation", "datecreation")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN datecreation date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                } else {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation ADD COLUMN date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                }
            }

            if (!columnExists("reclamation", "client_id")) {
                if (columnExists("reclamation", "user_id")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN user_id client_id INT NOT NULL");
                    }
                } else if (columnExists("reclamation", "id_client")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN id_client client_id INT NOT NULL");
                    }
                } else if (columnExists("reclamation", "clientid")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN clientid client_id INT NOT NULL");
                    }
                } else if (columnExists("reclamation", "client _id")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation CHANGE COLUMN `client _id` client_id INT NOT NULL");
                    }
                } else {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reclamation ADD COLUMN client_id INT NULL");
                    }
                }
            }

            // Compatibilite des schemas legacy pour reponse_reclamation.
            if (tableExists("reponse_reclamation")) {
                if (columnExists("reponse_reclamation", "datereponse") && !columnExists("reponse_reclamation", "date_reponse")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reponse_reclamation CHANGE COLUMN datereponse date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                } else if (columnExists("reponse_reclamation", "datecreation") && !columnExists("reponse_reclamation", "date_reponse")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reponse_reclamation CHANGE COLUMN datecreation date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                } else if (columnExists("reponse_reclamation", "date_creation") && !columnExists("reponse_reclamation", "date_reponse")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reponse_reclamation CHANGE COLUMN date_creation date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                } else if (columnExists("reponse_reclamation", "created_at") && !columnExists("reponse_reclamation", "date_reponse")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reponse_reclamation CHANGE COLUMN created_at date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                } else if (!columnExists("reponse_reclamation", "date_reponse")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reponse_reclamation ADD COLUMN date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    }
                }

                // S'assure que la colonne standard a un default valide.
                if (columnExists("reponse_reclamation", "date_reponse")) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE reponse_reclamation MODIFY COLUMN date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    } catch (SQLException ignored) {
                        // Selon les privileges, cette operation peut echouer sans bloquer l'app.
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de compatibilité du schéma reclamation : " + e.getMessage());
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    // ============ RECLAMATIONS ============

    // 1. AJOUTER UNE RÉCLAMATION
    public void ajouterReclamation(Reclamation r) throws SQLException {
        validerReclamation(r);
        appliquerModerationDescription(r);

        String sql = "INSERT INTO reclamation (titre, description, image, statut, date_creation, client_id) " +
                "VALUES (?, ?, ?, ?, NOW(), ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getTitre());
            ps.setString(2, r.getDescription());
            ps.setString(3, r.getImageUrl());
            ps.setString(4, r.getStatut());
            ps.setInt(5, r.getClientId());
            ps.executeUpdate();
        }
    }

    // 2. MODIFIER UNE RÉCLAMATION (par le client)
    public void modifierReclamation(Reclamation r) throws SQLException {
        validerReclamation(r);
        appliquerModerationDescription(r);

        String sql = "UPDATE reclamation SET titre=?, description=?, image=? WHERE id=? AND client_id=? AND statut='en_attente'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getTitre());
            ps.setString(2, r.getDescription());
            ps.setString(3, r.getImageUrl());
            ps.setInt(4, r.getId());
            ps.setInt(5, r.getClientId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Seules vos réclamations en attente peuvent être modifiées.");
            }
        }
    }

    // 3. MODIFIER LE STATUT (par l'admin)
    public void modifierStatut(int reclamationId, String nouveauStatut) throws SQLException {
        Reclamation.StatutReclamation.fromString(nouveauStatut); // validation

        String sql = "UPDATE reclamation SET statut=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, reclamationId);
            ps.executeUpdate();
        }
    }

    // 4. SUPPRIMER UNE RÉCLAMATION
    public void supprimerReclamation(int reclamationId) throws SQLException {
        // Supprimer d'abord les réponses associées
        String sqlReponses = "DELETE FROM reponse_reclamation WHERE reclamation_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlReponses)) {
            ps.setInt(1, reclamationId);
            ps.executeUpdate();
        }

        // Puis supprimer la réclamation
        String sql = "DELETE FROM reclamation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ps.executeUpdate();
        }
    }

    // 5. AFFICHER TOUTES LES RÉCLAMATIONS D'UN CLIENT
    public List<Reclamation> afficherReclamationsClient(int clientId) throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE client_id=? ORDER BY date_creation DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reclamation r = mapperReclamation(rs);
                    reclamations.add(r);
                }
            }
        }
        return reclamations;
    }

    // 6. AFFICHER TOUTES LES RÉCLAMATIONS (pour l'admin)
    public List<Reclamation> afficherToutesReclamations() throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT r.*, u.id as user_id, u.nom, u.prenom, u.email " +
                "FROM reclamation r " +
                "LEFT JOIN user u ON r.client_id = u.id " +
                "ORDER BY r.date_creation DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Reclamation r = mapperReclamation(rs);
                // Charger le client si disponible
                if (rs.getObject("user_id") != null) {
                    User client = new User();
                    client.setId(rs.getInt("user_id"));
                    client.setNom(rs.getString("nom"));
                    client.setPrenom(rs.getString("prenom"));
                    client.setEmail(rs.getString("email"));
                    r.setClient(client);
                }
                reclamations.add(r);
            }
        }
        return reclamations;
    }

    // 7. RÉCUPÉRER UNE RÉCLAMATION PAR ID
    public Reclamation obtenirReclamation(int id) throws SQLException {
        String sql = "SELECT r.*, u.id as user_id, u.nom, u.prenom, u.email " +
                "FROM reclamation r " +
                "LEFT JOIN user u ON r.client_id = u.id " +
                "WHERE r.id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reclamation r = mapperReclamation(rs);
                    if (rs.getObject("user_id") != null) {
                        User client = new User();
                        client.setId(rs.getInt("user_id"));
                        client.setNom(rs.getString("nom"));
                        client.setPrenom(rs.getString("prenom"));
                        client.setEmail(rs.getString("email"));
                        r.setClient(client);
                    }
                    return r;
                }
            }
        }
        return null;
    }

    // ============ RÉPONSES AUX RÉCLAMATIONS ============

    // 8. AJOUTER UNE RÉPONSE
    public void ajouterReponse(ReponseReclamation rep) throws SQLException {
        validerReponse(rep);
        rep.setContenu(modererTexteOuLever(rep.getContenu(), "réponse admin"));

        // Ecrit explicitement la date pour eviter les erreurs de schema legacy sans DEFAULT.
        String dateColumn = getReponseDateInsertColumn();
        String sql;
        if (dateColumn == null) {
            sql = "INSERT INTO reponse_reclamation (contenu, reclamation_id, admin_id) VALUES (?, ?, ?)";
        } else {
            sql = "INSERT INTO reponse_reclamation (contenu, " + dateColumn + ", reclamation_id, admin_id) VALUES (?, NOW(), ?, ?)";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rep.getContenu());
            ps.setInt(2, rep.getReclamationId());
            ps.setInt(3, rep.getAdminId());
            ps.executeUpdate();
        }
    }

    private String getReponseDateInsertColumn() {
        try {
            if (columnExists("reponse_reclamation", "date_reponse")) return "date_reponse";
            if (columnExists("reponse_reclamation", "datereponse")) return "datereponse";
            if (columnExists("reponse_reclamation", "date_creation")) return "date_creation";
            if (columnExists("reponse_reclamation", "datecreation")) return "datecreation";
            if (columnExists("reponse_reclamation", "created_at")) return "created_at";
        } catch (SQLException ignored) {
        }
        return null;
    }

    // 9. OBTENIR LES RÉPONSES POUR UNE RÉCLAMATION
    public List<ReponseReclamation> obtenirReponses(int reclamationId) throws SQLException {
        List<ReponseReclamation> reponses = new ArrayList<>();
        String sql = "SELECT rep.*, u.id as admin_id_check, u.nom, u.prenom, u.email " +
                "FROM reponse_reclamation rep " +
                "LEFT JOIN user u ON rep.admin_id = u.id " +
                "WHERE rep.reclamation_id=? " +
                "ORDER BY rep." + getReponseDateOrderColumn() + " ASC, rep.id ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReponseReclamation rep = new ReponseReclamation();
                    rep.setId(rs.getInt("id"));
                    rep.setContenu(rs.getString("contenu"));
                    rep.setDateReponse(readReponseDate(rs));
                    rep.setReclamationId(rs.getInt("reclamation_id"));
                    rep.setAdminId(rs.getInt("admin_id"));

                    if (rs.getObject("admin_id_check") != null) {
                        User admin = new User();
                        admin.setId(rs.getInt("admin_id_check"));
                        admin.setNom(rs.getString("nom"));
                        admin.setPrenom(rs.getString("prenom"));
                        admin.setEmail(rs.getString("email"));
                        rep.setAdmin(admin);
                    }
                    reponses.add(rep);
                }
            }
        }
        return reponses;
    }

    // 10. SUPPRIMER UNE RÉPONSE
    public void supprimerReponse(int reponseId) throws SQLException {
        String sql = "DELETE FROM reponse_reclamation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reponseId);
            ps.executeUpdate();
        }
    }

    // 10-bis. MODIFIER LA DERNIÈRE RÉPONSE D'UN ADMIN POUR UNE RÉCLAMATION
    public void modifierDerniereReponse(int reclamationId, int adminId, String nouveauContenu) throws SQLException {
        ReponseReclamation rep = new ReponseReclamation();
        rep.setReclamationId(reclamationId);
        rep.setAdminId(adminId);
        rep.setContenu(nouveauContenu);
        validerReponse(rep);

        String contenuModere = modererTexteOuLever(nouveauContenu, "réponse admin");

        int reponseId = trouverDerniereReponseId(reclamationId, adminId);
        if (reponseId <= 0) {
            throw new IllegalArgumentException("Aucune réponse existante à modifier. Ajoutez d'abord une réponse.");
        }

        String sql = "UPDATE reponse_reclamation SET contenu=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, contenuModere);
            ps.setInt(2, reponseId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("La réponse n'a pas pu être mise à jour.");
            }
        }
    }

    private int trouverDerniereReponseId(int reclamationId, int adminId) throws SQLException {
        String sql = "SELECT id FROM reponse_reclamation WHERE reclamation_id=? AND admin_id=? " +
                "ORDER BY " + getReponseDateOrderColumn() + " DESC, id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ps.setInt(2, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    private String getReponseDateOrderColumn() {
        try {
            if (columnExists("reponse_reclamation", "date_reponse")) {
                return "date_reponse";
            }
            if (columnExists("reponse_reclamation", "date_creation")) {
                return "date_creation";
            }
            if (columnExists("reponse_reclamation", "created_at")) {
                return "created_at";
            }
        } catch (SQLException ignored) {
        }
        return "id";
    }

    private Timestamp readReponseDate(ResultSet rs) throws SQLException {
        try {
            return rs.getTimestamp("date_reponse");
        } catch (SQLException ignored) {
        }
        try {
            return rs.getTimestamp("date_creation");
        } catch (SQLException ignored) {
        }
        try {
            return rs.getTimestamp("created_at");
        } catch (SQLException ignored) {
        }
        return null;
    }

    // ============ STATISTIQUES ============

    // 11. COMPTER LES RÉCLAMATIONS PAR STATUT
    public int compterReclamationsParStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reclamation WHERE statut=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // 12. COMPTER LES RÉCLAMATIONS EN ATTENTE DE RÉPONSE
    public int compterReclamationsSansReponse() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT r.id) FROM reclamation r " +
                "LEFT JOIN reponse_reclamation rep ON r.id = rep.reclamation_id " +
                "WHERE rep.id IS NULL";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // ============ MÉTHODES UTILITAIRES ============

    private void validerReclamation(Reclamation r) {
        if (r == null) {
            throw new IllegalArgumentException("Réclamation invalide.");
        }

        String titre = r.getTitre() == null ? "" : r.getTitre().trim();
        if (titre.isEmpty() || titre.length() < 3) {
            throw new IllegalArgumentException("Ce champ doit être rempli (entre 3 et 255 caractères)");
        }
        if (titre.length() > 255) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 255 caractères.");
        }

        // Description est optionnelle mais si fournie doit avoir au moins 10 caractères
        if (r.getDescription() != null) {
            String description = r.getDescription().trim();
            if (description.length() < 10) {
                throw new IllegalArgumentException("Au moins 10 caractères");
            }
            if (description.length() > 2000) {
                throw new IllegalArgumentException("La description ne peut pas dépasser 2000 caractères.");
            }
        }

        // Image est optionnelle mais si fournie doit être une image valide
        if (r.getImageUrl() != null && !r.getImageUrl().trim().isEmpty()) {
            String lower = r.getImageUrl().toLowerCase(Locale.ROOT);
            boolean isImage = lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".png")
                    || lower.endsWith(".gif")
                    || lower.endsWith(".webp")
                    || lower.contains(".jpg?")
                    || lower.contains(".jpeg?")
                    || lower.contains(".png?")
                    || lower.contains(".gif?")
                    || lower.contains(".webp?");
            if (!isImage) {
                throw new IllegalArgumentException("Le fichier image est invalide.");
            }
        }
    }

    private void validerReponse(ReponseReclamation rep) {
        if (rep == null) {
            throw new IllegalArgumentException("Réponse invalide.");
        }

        String contenu = rep.getContenu() == null ? "" : rep.getContenu().trim();
        if (contenu.isEmpty() || contenu.length() < 5) {
            throw new IllegalArgumentException("Le contenu doit contenir au moins 5 caractères.");
        }
        if (contenu.length() > 2000) {
            throw new IllegalArgumentException("Le contenu ne peut pas dépasser 2000 caractères.");
        }

        if (rep.getReclamationId() <= 0) {
            throw new IllegalArgumentException("ID de réclamation invalide.");
        }

        if (rep.getAdminId() <= 0) {
            throw new IllegalArgumentException("ID d'administrateur invalide.");
        }
    }

    private Reclamation mapperReclamation(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setTitre(rs.getString("titre"));
        r.setDescription(rs.getString("description"));
        r.setImageUrl(rs.getString("image"));
        r.setStatut(rs.getString("statut"));
        r.setDateCreation(rs.getTimestamp("date_creation"));
        r.setClientId(rs.getInt("client_id"));
        return r;
    }

    private void appliquerModerationDescription(Reclamation r) throws SQLException {
        if (r == null || r.getDescription() == null || r.getDescription().isBlank()) {
            return;
        }
        r.setDescription(modererTexteOuLever(r.getDescription(), "description de réclamation"));
    }

    private String modererTexteOuLever(String texte, String contexte) throws SQLException {
        BadWordService.ModerationResult result = badWordService.verifierCommentaire(texte);
        if (!result.isAllowed()) {
            throw new IllegalArgumentException(
                    "Le " + contexte + " contient des mots interdits: " + String.join(", ", result.getDetectedWords()));
        }
        return result.getFinalText();
    }
}

