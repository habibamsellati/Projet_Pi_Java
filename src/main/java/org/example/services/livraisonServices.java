package org.example.services;

import org.example.models.livraison;
import org.example.utils.MyDbConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class livraisonServices {
    private Connection cnx;

    public livraisonServices() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    // --- LOGIQUE POUR LE CALENDRIER & MÉTÉO ---
    public List<livraison> findByLivreur(int livreurId) throws SQLException {
        List<livraison> list = new ArrayList<>();
        String req = "SELECT * FROM `livraison` WHERE livreur_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, livreurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    livraison l = new livraison();
                    l.setId(rs.getInt("id"));
                    l.setAddressLivraison(rs.getString("addresslivraison"));
                    l.setStatutLivraison(rs.getString("statutlivraison"));

                    double lat = rs.getDouble("lat");
                    l.setLat(rs.wasNull() ? null : lat);
                    double lng = rs.getDouble("lng");
                    l.setLng(rs.wasNull() ? null : lng);

                    Timestamp ts = rs.getTimestamp("datelivraison");
                    if (ts != null) l.setDateLivraison(ts.toLocalDateTime());
                    list.add(l);
                }
            }
        }
        return list;
    }

    // --- MÉTHODES POUR LES FORMULAIRES (AjouterLivraison) ---
    public List<Integer> getAllCommandeIds() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery("SELECT id FROM `commande`")) {
            while (rs.next()) ids.add(rs.getInt("id"));
        }
        return ids;
    }

    public List<String> getEmailsLivreurs() throws SQLException {
        List<String> emails = new ArrayList<>();
        String req = "SELECT email FROM user WHERE role = 'livreur'";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) emails.add(rs.getString("email"));
        }
        return emails;
    }

    public Integer getIdByEmail(String email) throws SQLException {
        String req = "SELECT id FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    public String getEmailById(Integer id) throws SQLException {
        if (id == null || id == 0) return null;
        String req = "SELECT email FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("email");
            }
        }
        return null;
    }

    // --- GESTION DES MISSIONS & POSITION (EspaceLivreur) ---
    public void demarrerMission(Integer livraisonId) throws SQLException {
        updateStatut(livraisonId, "En cours");
    }

    public void terminerMission(Integer livraisonId) throws SQLException {
        updateStatut(livraisonId, "Livré");
    }

    public void updateStatut(int id, String nouveauStatut) throws SQLException {
        String sql = "UPDATE livraison SET statutlivraison = ? WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setString(1, nouveauStatut);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    // RÉTABLISSEMENT DE LA MÉTHODE MANQUANTE
    public void updateLivreurPosition(int livreurId, double lat, double lng) throws SQLException {
        String sql = "UPDATE user SET current_lat = ?, current_lng = ? WHERE id = ?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setDouble(1, lat);
            st.setDouble(2, lng);
            st.setInt(3, livreurId);
            st.executeUpdate();
        }
    }

    // --- CRUD DE BASE ---
    public List<livraison> findALL() throws SQLException {
        List<livraison> list = new ArrayList<>();
        String req = "SELECT l.*, u.current_lat, u.current_lng FROM `livraison` l LEFT JOIN user u ON l.livreur_id = u.id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                livraison l = new livraison();
                l.setId(rs.getInt("id"));
                l.setAddressLivraison(rs.getString("addresslivraison"));
                l.setStatutLivraison(rs.getString("statutlivraison"));
                l.setLat(rs.getObject("lat") != null ? rs.getDouble("lat") : null);
                l.setLng(rs.getObject("lng") != null ? rs.getDouble("lng") : null);
                Timestamp ts = rs.getTimestamp("datelivraison");
                if (ts != null) l.setDateLivraison(ts.toLocalDateTime());
                list.add(l);
            }
        }
        return list;
    }

    public void insertOne(livraison l) throws SQLException {
        String req = "INSERT INTO `livraison` (`datelivraison`, `addresslivraison`, `statutlivraison`, `note_livreur`, `livreur_id`, `lat`, `lng`, `commande_id`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            fillPreparedStatement(ps, l);
            ps.executeUpdate();
        }
    }

    public void updateOne(livraison l) throws SQLException {
        String req = "UPDATE `livraison` SET `datelivraison`=?, `addresslivraison`=?, `statutlivraison`=?, `note_livreur`=?, `livreur_id`=?, `lat`=?, `lng`=?, `commande_id`=? WHERE `id`=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            fillPreparedStatement(ps, l);
            ps.setInt(9, l.getId());
            ps.executeUpdate();
        }
    }

    private void fillPreparedStatement(PreparedStatement ps, livraison l) throws SQLException {
        if (l.getDateLivraison() != null) ps.setTimestamp(1, Timestamp.valueOf(l.getDateLivraison()));
        else ps.setNull(1, Types.TIMESTAMP);
        ps.setString(2, l.getAddressLivraison());
        ps.setString(3, l.getStatutLivraison());
        ps.setString(4, l.getNoteLivreur());
        if (l.getLivreurId() != null && l.getLivreurId() != 0) ps.setInt(5, l.getLivreurId());
        else ps.setNull(5, Types.INTEGER);
        if (l.getLat() != null) ps.setDouble(6, l.getLat()); else ps.setNull(6, Types.DOUBLE);
        if (l.getLng() != null) ps.setDouble(7, l.getLng()); else ps.setNull(7, Types.DOUBLE);
        if (l.getCommandeId() != null && l.getCommandeId() != 0) ps.setInt(8, l.getCommandeId());
        else ps.setNull(8, Types.INTEGER);
    }

    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `livraison` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}