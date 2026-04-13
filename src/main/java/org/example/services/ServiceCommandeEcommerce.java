package org.example.services;

import org.example.models.Commande;
import org.example.models.LigneCommandeArticle;
import org.example.utils.JdbcUtil;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Commandes multi-articles (panier) : total recalculé depuis les prix en base, lignes dans {@code ligne_commande}.
 */
public class ServiceCommandeEcommerce {
    public static final String STATUT_EN_ATTENTE = "en attente";

    private final Connection connection;

    public ServiceCommandeEcommerce() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    /**
     * Crée une commande client avec plusieurs lignes (article_id + quantité). Les prix unitaires sont lus sur {@code article}.
     */
    public Commande creerDepuisPanier(int clientId, String adresseLivraison, String modePaiement, String telephone,
                                      List<LigneCommandeArticle> lignes) throws SQLException {
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Le panier est vide.");
        }
        if (adresseLivraison == null || adresseLivraison.isBlank()) {
            throw new IllegalArgumentException("L'adresse de livraison est obligatoire.");
        }
        if (modePaiement == null || modePaiement.isBlank()) {
            throw new IllegalArgumentException("Le mode de paiement est obligatoire.");
        }
        String tel = (telephone == null || telephone.isBlank()) ? "—" : telephone.trim();

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            double total = 0;
            List<ResolvedLine> resolved = new ArrayList<>();
            for (LigneCommandeArticle ligne : lignes) {
                if (ligne.getQuantite() <= 0) {
                    throw new IllegalArgumentException("Quantité invalide pour l'article " + ligne.getArticleId());
                }
                double prix = getPrixArticleForUpdate(ligne.getArticleId());
                total += prix * ligne.getQuantite();
                resolved.add(new ResolvedLine(ligne.getArticleId(), ligne.getQuantite(), prix));
            }

            String numero = genererNumeroCommande();
            int commandeId = insertCommande(clientId, total, STATUT_EN_ATTENTE, adresseLivraison, tel, numero, modePaiement.trim());

            for (ResolvedLine ligne : resolved) {
                insertLigne(commandeId, ligne.articleId, ligne.quantite, ligne.prixUnitaire);
            }

            connection.commit();

            Commande c = new Commande();
            c.setId(commandeId);
            c.setUserId(clientId);
            c.setDateCommande(LocalDateTime.now());
            c.setTotal(total);
            c.setStatut(STATUT_EN_ATTENTE);
            c.setAdresseLivraison(adresseLivraison.trim());
            c.setTelephone(tel);
            c.setNumero(numero);
            c.setModePaiement(modePaiement.trim());
            return c;
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

    public List<Commande> listerPourClient(int userId) throws SQLException {
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commande WHERE user_id = ? ORDER BY date_commande DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCommande(rs));
                }
            }
        }
        return list;
    }

    /**
     * Commandes contenant au moins un article dont l’artisan est {@code artisanUserId} ({@code article.artisan_id} ou {@code article.user_id}).
     */
    public List<Commande> listerPourArtisan(int artisanUserId) throws SQLException {
        List<Commande> list = new ArrayList<>();
        String sql = """
                SELECT DISTINCT c.*
                FROM commande c
                INNER JOIN ligne_commande l ON l.commande_id = c.id
                INNER JOIN article a ON a.id = l.article_id
                WHERE a.artisan_id = ? OR a.user_id = ?
                ORDER BY c.date_commande DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artisanUserId);
            ps.setInt(2, artisanUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCommande(rs));
                }
            }
        }
        return list;
    }

    public void mettreAJourStatut(int commandeId, String nouveauStatut) throws SQLException {
        if (nouveauStatut == null || nouveauStatut.isBlank()) {
            throw new IllegalArgumentException("Statut invalide.");
        }
        String sql = "UPDATE commande SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut.trim());
            ps.setInt(2, commandeId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Commande introuvable.");
            }
        }
    }

    /** Annulation côté client (réutilise la logique article : lignes avec article_id). */
    public void annulerCommandeClient(int commandeId, int userId) throws SQLException {
        ServiceCommandeArticle delegate = new ServiceCommandeArticle();
        delegate.annulerCommandeClient(commandeId, userId);
    }

    private double getPrixArticleForUpdate(int articleId) throws SQLException {
        String sql = "SELECT prix FROM article WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Article introuvable (id=" + articleId + ").");
                }
                return rs.getDouble("prix");
            }
        }
    }

    private void insertLigne(int commandeId, int articleId, int quantite, double prixUnitaire) throws SQLException {
        String sql = "INSERT INTO ligne_commande (commande_id, article_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            ps.setInt(2, articleId);
            ps.setInt(3, quantite);
            ps.setDouble(4, prixUnitaire);
            ps.executeUpdate();
        }
    }

    private static String genererNumeroCommande() {
        return "CMD-" + System.currentTimeMillis();
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

    private record ResolvedLine(int articleId, int quantite, double prixUnitaire) {
    }
}
