package org.example.services;

import org.example.models.Commande;
import org.example.utils.JdbcUtil;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class ServiceCommande {
    public static final String STATUT_EN_ATTENTE = "en attente";
    public static final String STATUT_CONFIRMEE = "confirmée";
    public static final String STATUT_LIVREE = "livrée";
    public static final String STATUT_ANNULEE = "annulée";

    private final Connection connection;

    public ServiceCommande() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    public ProduitApercu getProduitApercu(int produitId) throws SQLException {
        String sql = "SELECT id, nom, image, quantite FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ProduitApercu(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("image"),
                        rs.getInt("quantite")
                );
            }
        }
    }

    public Commande creerCommande(int userId, int produitId, int quantiteDemandee, double prixUnitaire,
                                  String adresseLivraison, String telephone) throws SQLException {
        if (quantiteDemandee <= 0) {
            throw new IllegalArgumentException("La quantité commandée doit être positive.");
        }
        if (prixUnitaire < 0) {
            throw new IllegalArgumentException("Le prix unitaire ne peut pas être négatif.");
        }
        if (adresseLivraison == null || adresseLivraison.isBlank()) {
            throw new IllegalArgumentException("L'adresse de livraison est obligatoire.");
        }
        if (telephone == null || telephone.isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone est obligatoire.");
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int stockDisponible = getStockForUpdate(produitId);
            if (stockDisponible < quantiteDemandee) {
                throw new IllegalStateException("Stock insuffisant. Disponible: " + stockDisponible);
            }

            double total = quantiteDemandee * prixUnitaire;
            String numero = genererNumeroCommande();
            String modePaiement = "carte";
            int commandeId = insertCommande(userId, total, STATUT_EN_ATTENTE, adresseLivraison, telephone, numero, modePaiement);
            insertLigneCommande(commandeId, produitId, quantiteDemandee, prixUnitaire);
            decrementerStock(produitId, quantiteDemandee);

            connection.commit();

            Commande commande = new Commande();
            commande.setId(commandeId);
            commande.setUserId(userId);
            commande.setDateCommande(LocalDateTime.now());
            commande.setTotal(total);
            commande.setStatut(STATUT_EN_ATTENTE);
            commande.setAdresseLivraison(adresseLivraison);
            commande.setTelephone(telephone);
            commande.setNumero(numero);
            commande.setModePaiement(modePaiement);
            return commande;
        } catch (Exception e) {
            connection.rollback();
            if (e instanceof SQLException sqlEx) {
                throw sqlEx;
            }
            throw new SQLException(e.getMessage(), e);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private int getStockForUpdate(int produitId) throws SQLException {
        String sql = "SELECT quantite FROM produit WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Produit introuvable (id=" + produitId + ").");
                }
                return rs.getInt("quantite");
            }
        }
    }

    public void mettreAJourStatut(int commandeId, String nouveauStatut) throws SQLException {
        if (!STATUT_EN_ATTENTE.equals(nouveauStatut) &&
                !STATUT_CONFIRMEE.equals(nouveauStatut) &&
                !STATUT_LIVREE.equals(nouveauStatut)) {
            throw new IllegalArgumentException("Statut invalide.");
        }
        String sql = "UPDATE commande SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, commandeId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Commande introuvable.");
            }
        }
    }

    public void annulerCommandeClient(int commandeId, int userId) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String sqlCommande = "SELECT statut FROM commande WHERE id = ? AND user_id = ? FOR UPDATE";
            String statut;
            try (PreparedStatement ps = connection.prepareStatement(sqlCommande)) {
                ps.setInt(1, commandeId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Commande introuvable pour ce client.");
                    }
                    statut = rs.getString("statut");
                }
            }

            if (STATUT_LIVREE.equalsIgnoreCase(statut)) {
                throw new IllegalStateException("Annulation impossible: commande déjà livrée.");
            }

            String sqlLignes = "SELECT produit_id, quantite FROM ligne_commande WHERE commande_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlLignes)) {
                ps.setInt(1, commandeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int produitId = rs.getInt("produit_id");
                        int quantite = rs.getInt("quantite");
                        String sqlRestock = "UPDATE produit SET quantite = quantite + ? WHERE id = ?";
                        try (PreparedStatement psRestock = connection.prepareStatement(sqlRestock)) {
                            psRestock.setInt(1, quantite);
                            psRestock.setInt(2, produitId);
                            psRestock.executeUpdate();
                        }
                    }
                }
            }

            String sqlDelete = "DELETE FROM commande WHERE id = ? AND user_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlDelete)) {
                ps.setInt(1, commandeId);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }

            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            if (e instanceof SQLException sqlEx) {
                throw sqlEx;
            }
            throw new SQLException(e.getMessage(), e);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public List<Commande> afficherToutesLesCommandes() throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT * FROM commande ORDER BY date_commande DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                commandes.add(mapCommande(rs));
            }
        }
        return commandes;
    }

    public double getChiffreAffairesTotal() throws SQLException {
        String sql = "SELECT SUM(total) FROM commande WHERE statut != ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, STATUT_ANNULEE);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public int compterParStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE statut = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int compterTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private static String genererNumeroCommande() {
        return "CMD-" + System.currentTimeMillis();
    }

    private Commande mapCommande(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        Timestamp ts = rs.getTimestamp("date_commande");
        if (ts != null) {
            c.setDateCommande(ts.toLocalDateTime());
        }
        c.setTotal(rs.getDouble("total"));
        c.setStatut(rs.getString("statut"));
        c.setAdresseLivraison(rs.getString("adresse_livraison"));
        c.setTelephone(rs.getString("telephone"));
        if (JdbcUtil.hasColumn(rs, "numero")) {
            c.setNumero(rs.getString("numero"));
        }
        if (JdbcUtil.hasColumn(rs, "mode_paiement")) {
            c.setModePaiement(rs.getString("mode_paiement"));
        }
        return c;
    }

    private int insertCommande(int userId, double total, String statut, String adresseLivraison, String telephone,
                               String numero, String modePaiement) throws SQLException {
        String sql = "INSERT INTO commande (user_id, date_commande, total, statut, adresse_livraison, telephone, numero, mode_paiement) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, total);
            ps.setString(3, statut);
            ps.setString(4, adresseLivraison);
            ps.setString(5, telephone);
            ps.setString(6, numero);
            ps.setString(7, modePaiement);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            if (!JdbcUtil.isMissingColumnError(e)) {
                throw e;
            }
        }
        String sqlLegacy = "INSERT INTO commande (user_id, date_commande, total, statut, adresse_livraison, telephone) VALUES (?, NOW(), ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sqlLegacy, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, total);
            ps.setString(3, statut);
            ps.setString(4, adresseLivraison);
            ps.setString(5, telephone);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible de récupérer l'ID de la commande.");
    }

    private void insertLigneCommande(int commandeId, int produitId, int quantite, double prixUnitaire) throws SQLException {
        String sql = "INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            ps.setInt(2, produitId);
            ps.setInt(3, quantite);
            ps.setDouble(4, prixUnitaire);
            ps.executeUpdate();
        }
    }

    private void decrementerStock(int produitId, int quantiteAchetee) throws SQLException {
        String sql = "UPDATE produit SET quantite = quantite - ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, quantiteAchetee);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    public static class ProduitApercu {
        private final int id;
        private final String nom;
        private final String image;
        private final int stock;

        public ProduitApercu(int id, String nom, String image, int stock) {
            this.id = id;
            this.nom = nom;
            this.image = image;
            this.stock = stock;
        }

        public int getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public String getImage() {
            return image;
        }

        public int getStock() {
            return stock;
        }
    }
}
