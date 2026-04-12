package org.example.services;

import org.example.models.livraison;
import org.example.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class livraisonServices implements CRUD<livraison> {
    private Connection cnx;

    public livraisonServices() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(livraison livraison) throws SQLException {
        String req = "INSERT INTO `livraison` (`datelivraison`, `addresslivraison`, `statutlivraison`, `note_livreur`, `livreur_id`, `lat`, `lng`, `commande_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setTimestamp(1, Timestamp.valueOf(livraison.getDateLivraison()));
        ps.setString(2, livraison.getAddressLivraison());
        ps.setString(3, livraison.getStatutLivraison());
        ps.setString(4, livraison.getNoteLivreur());

        if (livraison.getLivreurId() != null) ps.setInt(5, livraison.getLivreurId());
        else ps.setNull(5, Types.INTEGER);

        if (livraison.getLat() != null) ps.setDouble(6, livraison.getLat());
        else ps.setNull(6, Types.DOUBLE);

        if (livraison.getLng() != null) ps.setDouble(7, livraison.getLng());
        else ps.setNull(7, Types.DOUBLE);

        if (livraison.getCommandeId() != null) ps.setInt(8, livraison.getCommandeId());
        else ps.setNull(8, Types.INTEGER);

        ps.executeUpdate();
    }

    @Override
    public void updateOne(livraison livraison) throws SQLException {
        // Mise à jour de TOUS les champs nécessaires pour ne pas perdre d'infos
        String req = "UPDATE `livraison` SET `datelivraison`=?, `addresslivraison`=?, `statutlivraison`=?, `note_livreur`=?, `livreur_id`=?, `commande_id`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setTimestamp(1, Timestamp.valueOf(livraison.getDateLivraison()));
        ps.setString(2, livraison.getAddressLivraison());
        ps.setString(3, livraison.getStatutLivraison());
        ps.setString(4, livraison.getNoteLivreur());

        if (livraison.getLivreurId() != null) ps.setInt(5, livraison.getLivreurId());
        else ps.setNull(5, Types.INTEGER);

        if (livraison.getCommandeId() != null) ps.setInt(6, livraison.getCommandeId());
        else ps.setNull(6, Types.INTEGER);

        ps.setInt(7, livraison.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `livraison` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<livraison> findALL() throws SQLException {
        List<livraison> list = new ArrayList<>();
        String req = "SELECT * FROM `livraison`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            livraison l = new livraison();
            l.setId(rs.getInt("id"));
            Timestamp ts = rs.getTimestamp("datelivraison");
            if (ts != null) l.setDateLivraison(ts.toLocalDateTime());

            l.setAddressLivraison(rs.getString("addresslivraison"));
            l.setStatutLivraison(rs.getString("statutlivraison"));
            l.setNoteLivreur(rs.getString("note_livreur"));
            l.setLivreurId(rs.getObject("livreur_id") != null ? rs.getInt("livreur_id") : null);
            l.setLat(rs.getObject("lat") != null ? rs.getDouble("lat") : null);
            l.setLng(rs.getObject("lng") != null ? rs.getDouble("lng") : null);
            l.setCommandeId(rs.getObject("commande_id") != null ? rs.getInt("commande_id") : null);
            list.add(l);
        }
        return list;
    }

    // --- Méthodes Utilitaires pour les ComboBox ---

    public List<Integer> getAllCommandeIds() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String req = "SELECT id FROM `commande`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) ids.add(rs.getInt("id"));
        return ids;
    }

    public List<String> getEmailsLivreurs() throws SQLException {
        List<String> emails = new ArrayList<>();
        String req = "SELECT email FROM user WHERE role = 'livreur'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) emails.add(rs.getString("email"));
        return emails;
    }

    public Integer getIdByEmail(String email) throws SQLException {
        if (email == null) return null;
        String req = "SELECT id FROM user WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("id") : null;
    }

    // AJOUT DE LA MÉTHODE MANQUANTE
    public String getEmailById(Integer id) throws SQLException {
        if (id == null) return null;
        String req = "SELECT email FROM user WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getString("email") : null;
    }
}