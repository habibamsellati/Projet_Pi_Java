package org.example.services;

import org.example.models.Evenement;
import org.example.utils.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvenement {

    private final Connection connection;

    public ServiceEvenement() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    public void ajouter(Evenement e) throws SQLException {
        valider(e, true);
        if (e.getStatut() == null || e.getStatut().isBlank()) {
            e.setStatut("brouillon");
        }
        String sql = "INSERT INTO evenement (nom, artisan, description, date_debut, date_fin, lieu, capacite, type_art, theme, prix, image, statut, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom().trim());
            setStringOrNull(ps, 2, e.getArtisan());
            ps.setString(3, e.getDescription().trim());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            ps.setString(6, e.getLieu().trim());
            ps.setInt(7, e.getCapacite());
            setStringOrNull(ps, 8, e.getTypeArt());
            setStringOrNull(ps, 9, e.getTheme());
            if (e.getPrix() != null) {
                ps.setBigDecimal(10, e.getPrix());
            } else {
                ps.setNull(10, Types.DECIMAL);
            }
            setStringOrNull(ps, 11, e.getImage());
            ps.setString(12, e.getStatut());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
        }
    }

    public void modifier(Evenement e) throws SQLException {
        if (e.getId() <= 0) throw new IllegalArgumentException("ID événement invalide.");
        valider(e, false);
        String sql = "UPDATE evenement SET nom=?, artisan=?, description=?, date_debut=?, date_fin=?, lieu=?, capacite=?, type_art=?, theme=?, prix=?, image=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getNom().trim());
            setStringOrNull(ps, 2, e.getArtisan());
            ps.setString(3, e.getDescription().trim());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            ps.setString(6, e.getLieu().trim());
            ps.setInt(7, e.getCapacite());
            setStringOrNull(ps, 8, e.getTypeArt());
            setStringOrNull(ps, 9, e.getTheme());
            if (e.getPrix() != null) ps.setBigDecimal(10, e.getPrix());
            else ps.setNull(10, Types.DECIMAL);
            setStringOrNull(ps, 11, e.getImage());
            ps.setString(12, e.getStatut());
            ps.setInt(13, e.getId());
            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("Événement introuvable.");
        }
    }

    public void supprimer(int id) throws SQLException {
        // Suppression manuelle en cascade : on supprime d'abord les images de l'événement
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM evenement_image WHERE evenement_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException ignore) { /* La table n'existe peut-être pas */ }
        
        // Ensuite on supprime les réservations associées à cet événement
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM reservation WHERE evenement_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException ignore) {}

        // Et on termine par supprimer l'événement (ligne parent)
        String sql = "DELETE FROM evenement WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Evenement trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    /** Liste triée par date de début (croissant). */
    public List<Evenement> listerTousParDateDebutAsc() throws SQLException {
        return lister("ORDER BY date_debut ASC");
    }

    public List<Evenement> listerTousParDateDebutDesc() throws SQLException {
        return lister("ORDER BY date_debut DESC");
    }

    private List<Evenement> lister(String orderClause) throws SQLException {
        List<Evenement> list = new ArrayList<Evenement>();
        String sql = "SELECT * FROM evenement " + orderClause;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int compterTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int sommeCapaciteTotale() throws SQLException {
        String sql = "SELECT SUM(capacite) FROM evenement";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Evenement map(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));
        e.setNom(rs.getString("nom"));
        e.setArtisan(rs.getString("artisan"));
        e.setDescription(rs.getString("description"));
        Timestamp t = rs.getTimestamp("date_debut");
        if (t != null) e.setDateDebut(t.toLocalDateTime());
        t = rs.getTimestamp("date_fin");
        if (t != null) e.setDateFin(t.toLocalDateTime());
        e.setLieu(rs.getString("lieu"));
        e.setCapacite(rs.getInt("capacite"));
        e.setTypeArt(rs.getString("type_art"));
        e.setTheme(rs.getString("theme"));
        BigDecimal p = rs.getBigDecimal("prix");
        e.setPrix(rs.wasNull() ? null : p);
        e.setImage(rs.getString("image"));
        e.setStatut(rs.getString("statut"));
        t = rs.getTimestamp("created_at");
        if (t != null) e.setCreatedAt(t.toLocalDateTime());
        return e;
    }

    private void valider(Evenement e, boolean exigerDatesFuturesDebut) {
        if (e.getNom() == null || e.getNom().trim().length() < 3 || e.getNom().trim().length() > 255) {
            throw new IllegalArgumentException("Le nom doit contenir entre 3 et 255 caractères.");
        }
        if (e.getDescription() == null || e.getDescription().trim().length() < 10 || e.getDescription().trim().length() > 5000) {
            throw new IllegalArgumentException("La description doit contenir entre 10 et 5000 caractères.");
        }
        if (e.getDateDebut() == null || e.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et fin sont obligatoires.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (exigerDatesFuturesDebut && e.getDateDebut().isBefore(now)) {
            throw new IllegalArgumentException("La date de début ne peut pas être dans le passé.");
        }
        if (e.getDateFin().isBefore(e.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être >= à la date de début.");
        }
        if (e.getLieu() == null || e.getLieu().trim().length() < 3 || e.getLieu().trim().length() > 255) {
            throw new IllegalArgumentException("Le lieu doit contenir entre 3 et 255 caractères.");
        }
        if (e.getCapacite() < 1 || e.getCapacite() > 10000) {
            throw new IllegalArgumentException("La capacité doit être entre 1 et 10000.");
        }
    }

    private static void setStringOrNull(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v.trim());
    }
}
