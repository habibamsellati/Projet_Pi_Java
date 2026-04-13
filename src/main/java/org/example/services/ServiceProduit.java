package org.example.services;

import org.example.models.Produit;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit {
    private final Connection connection;

    public ServiceProduit() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    // 1. AJOUTER UN PRODUIT (Produit Recyclable)
    public void ajouter(Produit p) throws SQLException {
        String sql = "INSERT INTO produit (nom, type_materiau, etat, quantite, origine, description, image, impact_ecologique, artisan_id, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getTypeMateriau());
            ps.setString(3, p.getEtat());
            ps.setInt(4, p.getQuantite());
            ps.setString(5, p.getOrigine());
            ps.setString(6, p.getDescription());
            ps.setString(7, p.getImage());
            ps.setInt(8, p.getImpactEcologique());

            int artisanId = (p.getArtisanId() == 0) ? 3 : p.getArtisanId();
            int userId = (p.getUserId() == 0) ? 3 : p.getUserId();
            ps.setInt(9, artisanId);
            ps.setInt(10, userId);

            ps.executeUpdate();
        }
    }

    // 2. MODIFIER UN PRODUIT
    public void modifier(Produit p) throws SQLException {
        String sql = "UPDATE produit SET nom=?, type_materiau=?, etat=?, quantite=?, origine=?, description=?, image=?, impact_ecologique=?, artisan_id=?, user_id=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getTypeMateriau());
            ps.setString(3, p.getEtat());
            ps.setInt(4, p.getQuantite());
            ps.setString(5, p.getOrigine());
            ps.setString(6, p.getDescription());
            ps.setString(7, p.getImage());
            ps.setInt(8, p.getImpactEcologique());
            ps.setInt(9, p.getArtisanId());
            ps.setInt(10, p.getUserId());
            ps.setInt(11, p.getId());
            ps.executeUpdate();
        }
    }

    // 3. SUPPRIMER UN PRODUIT
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // 4. AFFICHER LES PRODUITS
    public List<Produit> afficher() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Produit p = new Produit();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setTypeMateriau(rs.getString("type_materiau"));
                p.setEtat(rs.getString("etat"));
                p.setQuantite(rs.getInt("quantite"));
                p.setOrigine(rs.getString("origine"));
                p.setDescription(rs.getString("description"));
                p.setImage(rs.getString("image"));
                p.setImpactEcologique(rs.getInt("impact_ecologique"));
                p.setArtisanId(rs.getInt("artisan_id"));
                p.setUserId(rs.getInt("user_id"));
                produits.add(p);
            }
        }
        return produits;
    }
}

