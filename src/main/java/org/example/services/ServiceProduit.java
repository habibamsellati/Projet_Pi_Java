package org.example.services;

import org.example.models.Produit;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service produit adapté à la vraie structure de la BD :
 * nomproduit, typemateriau, etat, quantite, origine,
 * impactecologique, dateajout, description, added_by_id, image
 */
public class ServiceProduit {

    private final Connection connection;

    public ServiceProduit() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public void ajouter(Produit p) throws SQLException {
        validerProduit(p);
        String sql = "INSERT INTO produit (nomproduit, typemateriau, etat, quantite, origine, " +
                     "description, image, impactecologique, added_by_id, dateajout) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, safe(p.getNom()));
            ps.setString(2, safe(p.getTypeMateriau()));
            ps.setString(3, safe(p.getEtat()));
            ps.setInt(4, p.getQuantite());
            ps.setString(5, safe(p.getOrigine()));
            ps.setString(6, safe(p.getDescription()));
            ps.setString(7, safe(p.getImage()));
            ps.setDouble(8, p.getImpactEcologique());
            ps.setInt(9, p.getArtisanId() > 0 ? p.getArtisanId() : 3);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
    }

    public void modifier(Produit p) throws SQLException {
        if (p == null || p.getId() <= 0) throw new IllegalArgumentException("Produit invalide.");
        validerProduit(p);
        String sql = "UPDATE produit SET nomproduit=?, typemateriau=?, etat=?, quantite=?, " +
                     "origine=?, description=?, image=?, impactecologique=?, added_by_id=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, safe(p.getNom()));
            ps.setString(2, safe(p.getTypeMateriau()));
            ps.setString(3, safe(p.getEtat()));
            ps.setInt(4, p.getQuantite());
            ps.setString(5, safe(p.getOrigine()));
            ps.setString(6, safe(p.getDescription()));
            ps.setString(7, safe(p.getImage()));
            ps.setDouble(8, p.getImpactEcologique());
            ps.setInt(9, p.getArtisanId() > 0 ? p.getArtisanId() : 3);
            ps.setInt(10, p.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        // Règle métier : blocage si propositions liées
        ServiceProposition sp = new ServiceProposition();
        if (!sp.produitPeutEtreSupprime(id)) {
            throw new IllegalArgumentException(
                "Impossible de supprimer ce produit : il a des propositions liées. " +
                "Supprimez d'abord les propositions associées.");
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM produit WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Produit> afficher() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit ORDER BY dateajout DESC, id DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) produits.add(mapProduit(rs));
        }
        return produits;
    }

    public List<Produit> afficherParArtisan(int artisanId) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit WHERE added_by_id = ? ORDER BY dateajout DESC, id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) produits.add(mapProduit(rs));
            }
        }
        return produits;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Produit mapProduit(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nomproduit"));
        p.setTypeMateriau(rs.getString("typemateriau"));
        p.setEtat(rs.getString("etat"));
        p.setQuantite(rs.getInt("quantite"));
        p.setOrigine(rs.getString("origine"));
        p.setDescription(rs.getString("description"));
        p.setImage(rs.getString("image"));
        p.setImpactEcologique((int) rs.getDouble("impactecologique"));
        p.setArtisanId(rs.getInt("added_by_id"));
        try { p.setDateCreation(rs.getTimestamp("dateajout")); }
        catch (SQLException ignored) {}
        return p;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validerProduit(Produit p) {
        if (p == null) throw new IllegalArgumentException("Produit invalide.");
        if (p.getNom() == null || p.getNom().trim().length() < 2)
            throw new IllegalArgumentException("Le nom doit contenir au moins 2 caractères.");
        if (p.getDescription() == null || p.getDescription().trim().length() < 10)
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères.");
        if (p.getQuantite() < 0)
            throw new IllegalArgumentException("La quantité doit être positive ou nulle.");
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
