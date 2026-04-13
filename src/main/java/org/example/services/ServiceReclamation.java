package org.example.services;

import org.example.models.Reclamation;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReclamation {
    public static final String STATUT_EN_ATTENTE = "en_attente";
    public static final String STATUT_EN_COURS = "en_cours";
    public static final String STATUT_RESOLU = "resolu";
    public static final String STATUT_REJETE = "rejete";

    private final Connection connection;

    public ServiceReclamation() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    public void ajouter(Reclamation r) throws SQLException {
        valider(r);
        String sql = "INSERT INTO reclamation (user_id, titre, descripition, image, datecreation, statut) VALUES (?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, r.getUserId() == 0 ? 3 : r.getUserId());
            ps.setString(2, r.getTitre().trim());
            ps.setString(3, nullIfBlank(r.getDescripition()));
            ps.setString(4, nullIfBlank(r.getImage()));
            ps.setString(5, (r.getStatut() == null || r.getStatut().isBlank()) ? STATUT_EN_ATTENTE : r.getStatut());
            ps.executeUpdate();
        }
    }

    public void modifier(Reclamation r) throws SQLException {
        if (r.getId() <= 0) throw new IllegalArgumentException("ID réclamation invalide.");
        valider(r);
        String sql = "UPDATE reclamation SET titre=?, descripition=?, image=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getTitre().trim());
            ps.setString(2, nullIfBlank(r.getDescripition()));
            ps.setString(3, nullIfBlank(r.getImage()));
            ps.setString(4, r.getStatut());
            ps.setInt(5, r.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Reclamation> afficherParUser(int userId) throws SQLException {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE user_id = ? ORDER BY datecreation DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Reclamation map(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setTitre(rs.getString("titre"));
        r.setDescripition(rs.getString("descripition"));
        r.setImage(rs.getString("image"));
        Timestamp ts = rs.getTimestamp("datecreation");
        if (ts != null) r.setDateCreation(ts.toLocalDateTime());
        r.setStatut(rs.getString("statut"));
        return r;
    }

    private void valider(Reclamation r) {
        if (r.getTitre() == null || r.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre est obligatoire.");
        }
        String d = r.getDescripition();
        if (d != null && !d.isBlank() && d.trim().length() < 10) {
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères.");
        }
        if (r.getStatut() == null || r.getStatut().isBlank()) {
            r.setStatut(STATUT_EN_ATTENTE);
        }
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

