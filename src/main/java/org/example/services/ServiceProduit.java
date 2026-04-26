package org.example.services;

import org.example.models.Produit;
import org.example.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit {
    private Connection connection;

    public ServiceProduit() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Produit p) throws SQLException {
        String sql = "INSERT INTO produit (nom, type_materiau, etat, quantite, origine, description, image, impact_ecologique, artisan_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getTypeMateriau());
        ps.setString(3, p.getEtat());
        ps.setInt(4, p.getQuantite());
        ps.setString(5, p.getOrigine());
        ps.setString(6, p.getDescription());
        ps.setString(7, p.getImage());
        ps.setInt(8, p.getImpactEcologique());
        ps.setInt(9, p.getArtisanId());
        ps.executeUpdate();
    }

    public List<Produit> afficher() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Produit p = new Produit();
            p.setId(rs.getInt("id"));
            p.setNom(rs.getString("nom"));
            p.setQuantite(rs.getInt("quantite"));
            p.setImpactEcologique(rs.getInt("impact_ecologique"));
            p.setImage(rs.getString("image"));
            produits.add(p);
        }
        return produits;
    }
}