package org.example.services;

import org.example.models.Proposition;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service proposition avec règles métier :
 * - SMS automatique à chaque changement de statut
 * - Quota max 2 acceptations/jour pour l'artisan
 * - Blocage suppression produit si propositions liées
 */
public class ServiceProposition {

    private static final Set<String> STATUTS_AUTORISES =
            Set.of("en_attente", "acceptee", "refusee", "en_revision", "terminee");
    private static final int QUOTA_ACCEPTATIONS_PAR_JOUR = 2;

    private final Connection connection;
    private final EmailService emailService; // Email remplace SMS

    public ServiceProposition() {
        connection = MyDatabase.getInstance().getConnection();
        emailService = new EmailService();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public void ajouter(Proposition p) throws SQLException {
        validerProposition(p);
        String sql = "INSERT INTO proposition (produit_id, user_id, titre, description, " +
                     "prix_propose, client_phone, image, statut, datesoumision) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getProduitId());
            ps.setInt(2, p.getUserId());
            ps.setString(3, safe(p.getTitre()));
            ps.setString(4, safe(p.getDescription()));
            ps.setDouble(5, p.getPrixPropose());
            ps.setString(6, safe(p.getTelephone()));
            ps.setString(7, safe(p.getImage()));
            ps.setString(8, p.getStatut() == null ? "en_attente" : p.getStatut());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
    }

    public void modifier(Proposition p) throws SQLException {
        if (p == null || p.getId() <= 0) throw new IllegalArgumentException("Proposition invalide.");
        validerProposition(p);
        String sql = "UPDATE proposition SET produit_id=?, user_id=?, titre=?, description=?, " +
                     "prix_propose=?, client_phone=?, image=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, p.getProduitId());
            ps.setInt(2, p.getUserId());
            ps.setString(3, safe(p.getTitre()));
            ps.setString(4, safe(p.getDescription()));
            ps.setDouble(5, p.getPrixPropose());
            ps.setString(6, safe(p.getTelephone()));
            ps.setString(7, safe(p.getImage()));
            ps.setString(8, p.getStatut() == null ? "en_attente" : p.getStatut());
            ps.setInt(9, p.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM proposition WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Change le statut d'une proposition avec :
     * - Vérification quota acceptation (max 2/jour)
     * - Envoi SMS automatique au client
     *
     * @param propositionId  ID de la proposition
     * @param nouveauStatut  acceptee | refusee | terminee | en_revision
     * @param artisanId      ID de l'artisan qui effectue l'action (pour quota)
     */
    public void changerStatut(int propositionId, String nouveauStatut, int artisanId) throws SQLException {
        if (!STATUTS_AUTORISES.contains(nouveauStatut))
            throw new IllegalArgumentException("Statut invalide : " + nouveauStatut);

        // Règle : quota max 2 acceptations/jour
        if ("acceptee".equals(nouveauStatut) && artisanId > 0) {
            int nbAujourdhui = compterAcceptationsAujourdhui(artisanId);
            if (nbAujourdhui >= QUOTA_ACCEPTATIONS_PAR_JOUR) {
                throw new IllegalArgumentException(
                    "Quota atteint : vous ne pouvez accepter que " + QUOTA_ACCEPTATIONS_PAR_JOUR +
                    " propositions par jour. Réessayez demain.");
            }
        }

        // Récupérer la proposition pour l'email
        Proposition p = findById(propositionId);

        // Mettre à jour le statut
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE proposition SET statut=? WHERE id=?")) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, propositionId);
            ps.executeUpdate();
        }

        // Envoyer email au client (toujours, pas besoin de téléphone)
        if (p != null) {
            String nomProduit = p.getNomProduit() != null ? p.getNomProduit() : "votre produit";
            System.out.println("[cServiceProposition] Recherche email pour userId=" + p.getUserId()
                    + " (proposition #" + propositionId + ", statut=" + nouveauStatut + ")");
            String emailClient = obtenirEmailClient(p.getUserId());
            if (emailClient != null && !emailClient.isBlank()) {
                switch (nouveauStatut) {
                    case "acceptee" -> emailService.envoyerStatutProposition(emailClient,
                            p.getTitre(), nomProduit, "acceptee", p.getPrixPropose());
                    case "refusee"  -> emailService.envoyerStatutProposition(emailClient,
                            p.getTitre(), nomProduit, "refusee", p.getPrixPropose());
                    case "terminee" -> emailService.envoyerStatutProposition(emailClient,
                            p.getTitre(), nomProduit, "terminee", p.getPrixPropose());
                }
                System.out.println("[ServiceProposition] ✅ Email envoyé à " + emailClient
                        + " pour statut " + nouveauStatut);
            } else {
                System.err.println("[ServiceProposition] ❌ Email non envoyé — email introuvable pour userId=" + p.getUserId());
            }
        }
    }

    /** Surcharge sans artisanId (pas de vérification quota) */
    public void changerStatut(int id, String statut) throws SQLException {
        changerStatut(id, statut, 0);
    }

    // ── Règles métier ─────────────────────────────────────────────────────────

    /**
     * Vérifie si un produit peut être supprimé (pas de propositions liées).
     */
    public boolean produitPeutEtreSupprime(int produitId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM proposition WHERE produit_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }

    /**
     * Compte les acceptations faites aujourd'hui par un artisan.
     */
    public int compterAcceptationsAujourdhui(int artisanId) throws SQLException {
        // On compte les propositions acceptées aujourd'hui dont le produit appartient à cet artisan
        String sql = "SELECT COUNT(*) FROM proposition p " +
                     "JOIN produit pr ON p.produit_id = pr.id " +
                     "WHERE pr.added_by_id = ? " +
                     "AND p.statut = 'acceptee' " +
                     "AND DATE(p.datesoumision) = CURDATE()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    public Proposition findById(int id) throws SQLException {
        String sql = "SELECT p.*, pr.nomproduit AS produit_nom " +
                     "FROM proposition p LEFT JOIN produit pr ON p.produit_id = pr.id " +
                     "WHERE p.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Proposition> afficher() throws SQLException {
        String sql = "SELECT p.*, pr.nomproduit AS produit_nom " +
                     "FROM proposition p LEFT JOIN produit pr ON p.produit_id = pr.id " +
                     "ORDER BY p.datesoumision DESC, p.id DESC";
        return execQuery(sql);
    }

    public List<Proposition> afficherParUtilisateur(int userId) throws SQLException {
        String sql = "SELECT p.*, pr.nomproduit AS produit_nom " +
                     "FROM proposition p LEFT JOIN produit pr ON p.produit_id = pr.id " +
                     "WHERE p.user_id = ? ORDER BY p.datesoumision DESC, p.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return mapList(ps.executeQuery());
        }
    }

    public List<Proposition> afficherParProduit(int produitId) throws SQLException {
        String sql = "SELECT p.*, pr.nomproduit AS produit_nom " +
                     "FROM proposition p LEFT JOIN produit pr ON p.produit_id = pr.id " +
                     "WHERE p.produit_id = ? ORDER BY p.datesoumision DESC, p.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            return mapList(ps.executeQuery());
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private List<Proposition> execQuery(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        }
    }

    private List<Proposition> mapList(ResultSet rs) throws SQLException {
        List<Proposition> list = new ArrayList<>();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    private Proposition map(ResultSet rs) throws SQLException {
        Proposition p = new Proposition();
        p.setId(rs.getInt("id"));
        p.setProduitId(rs.getInt("produit_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setTitre(rs.getString("titre"));
        p.setDescription(rs.getString("description"));
        p.setPrixPropose(rs.getDouble("prix_propose"));
        p.setTelephone(rs.getString("client_phone"));
        p.setImage(rs.getString("image"));
        p.setStatut(rs.getString("statut"));
        p.setNomProduit(rs.getString("produit_nom"));
        try { p.setDateCreation(rs.getTimestamp("datesoumision")); }
        catch (SQLException ignored) {}
        return p;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validerProposition(Proposition p) {
        if (p == null) throw new IllegalArgumentException("Proposition invalide.");
        if (p.getProduitId() <= 0) throw new IllegalArgumentException("Veuillez sélectionner un produit.");
        if (p.getUserId() <= 0) throw new IllegalArgumentException("Utilisateur invalide.");
        if (p.getTitre() == null || p.getTitre().trim().length() < 3)
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères.");
        if (p.getDescription() == null || p.getDescription().trim().length() < 10)
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères.");
        if (p.getPrixPropose() < 0)
            throw new IllegalArgumentException("Le prix doit être positif ou nul.");
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String obtenirEmailClient(int userId) {
        if (userId <= 0) return null;

        // Priorité 1 : session courante si c'est le même utilisateur
        org.example.models.User sessionUser = org.example.utils.SessionManager.getCurrentUser();
        // (on ne prend pas la session ici car c'est l'artisan qui est connecté, pas le client)

        // Priorité 2 : base de données — table users
        String[] tables = {"users", "user"}; // essayer les deux noms possibles
        for (String table : tables) {
            try {
                String sql = "SELECT email FROM " + table + " WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String email = rs.getString("email");
                            if (email != null && !email.isBlank()) {
                                System.out.println("[ServiceProposition] Email client trouvé dans table '" + table + "': " + email);
                                return email.trim();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("[ServiceProposition] Table '" + table + "' inaccessible: " + e.getMessage());
            }
        }
        System.err.println("[ServiceProposition] Email introuvable pour userId=" + userId);
        return null;
    }
}
