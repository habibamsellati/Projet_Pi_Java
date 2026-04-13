package org.example.services;

import org.example.models.Commande;
import org.example.utils.JdbcUtil;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class ServiceCommandeArticle {
    public static final String STATUT_EN_ATTENTE = "en attente";
    public static final String STATUT_CONFIRMEE = "confirmée";
    public static final String STATUT_LIVREE = "livrée";

    private final Connection connection;

    public ServiceCommandeArticle() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    public ArticleApercu getArticleApercu(int articleId) throws SQLException {
        String sql = "SELECT id, titre, prix, image FROM article WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ArticleApercu(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getDouble("prix"),
                        rs.getString("image")
                );
            }
        }
    }

    /**
     * Crée une commande pour 1 article (à l'unité) et marque l'article comme vendu
     * (via colonne statut='vendu' si elle existe, sinon via quantite-- si elle existe).
     */
    public Commande creerCommandeArticle(int userId, int articleId, int quantiteDemandee,
                                         String adresseLivraison, String telephone) throws SQLException {
        if (quantiteDemandee <= 0) throw new IllegalArgumentException("La quantité doit être positive.");
        if (adresseLivraison == null || adresseLivraison.isBlank())
            throw new IllegalArgumentException("L'adresse de livraison est obligatoire.");
        if (telephone == null || telephone.isBlank())
            throw new IllegalArgumentException("Le numéro de téléphone est obligatoire.");

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            ArticleRow article = getArticleForUpdate(articleId);

            // Vente à l'unité: si pas de colonne quantite, on limite à 1.
            if (!article.hasQuantite && quantiteDemandee != 1) {
                throw new IllegalStateException("Cet article est vendu à l'unité (quantité = 1).");
            }

            // Vérif disponibilité
            if (article.hasStatut && "vendu".equalsIgnoreCase(article.statut)) {
                throw new IllegalStateException("Article déjà vendu.");
            }
            if (article.hasQuantite && article.quantite < quantiteDemandee) {
                throw new IllegalStateException("Stock insuffisant. Disponible: " + article.quantite);
            }

            double prixUnitaire = article.prix;
            double total = quantiteDemandee * prixUnitaire;

            String numero = genererNumeroCommande();
            String modePaiement = "carte";
            int commandeId = insertCommande(userId, total, STATUT_EN_ATTENTE, adresseLivraison, telephone, numero, modePaiement);
            insertLigneCommandeArticle(commandeId, articleId, quantiteDemandee, prixUnitaire);

            // Mise à jour article (vendu / quantite--)
            appliquerVenteArticle(articleId, quantiteDemandee, article);

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
            if (e instanceof SQLException sqlEx) throw sqlEx;
            throw new SQLException(e.getMessage(), e);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
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
            if (updated == 0) throw new IllegalArgumentException("Commande introuvable.");
        }
    }

    public void annulerCommandeClient(int commandeId, int userId) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String sqlCmd = "SELECT statut FROM commande WHERE id = ? AND user_id = ? FOR UPDATE";
            String statut;
            try (PreparedStatement ps = connection.prepareStatement(sqlCmd)) {
                ps.setInt(1, commandeId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Commande introuvable pour ce client.");
                    statut = rs.getString("statut");
                }
            }
            if (STATUT_LIVREE.equalsIgnoreCase(statut)) {
                throw new IllegalStateException("Annulation impossible: commande déjà livrée.");
            }

            // Restock / un-sell (best effort): si article a quantite, on ajoute; sinon on tente statut='disponible'
            String sqlLignes = "SELECT article_id, quantite FROM ligne_commande WHERE commande_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlLignes)) {
                ps.setInt(1, commandeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int articleId = rs.getInt("article_id");
                        int qte = rs.getInt("quantite");
                        tryRestockArticle(articleId, qte);
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
            if (e instanceof SQLException sqlEx) throw sqlEx;
            throw new SQLException(e.getMessage(), e);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private ArticleRow getArticleForUpdate(int articleId) throws SQLException {
        String sql = "SELECT * FROM article WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Article introuvable (id=" + articleId + ").");

                Set<String> cols = getColumns(rs.getMetaData());
                double prix = safeGetDouble(rs, "prix", cols);
                String statut = safeGetString(rs, "statut", cols);
                Integer quantite = safeGetInt(rs, "quantite", cols);

                if (prix < 0) throw new IllegalStateException("Prix article invalide.");

                return new ArticleRow(
                        prix,
                        cols.contains("statut"),
                        statut,
                        cols.contains("quantite"),
                        quantite == null ? 0 : quantite
                );
            }
        }
    }

    private void appliquerVenteArticle(int articleId, int quantiteDemandee, ArticleRow article) throws SQLException {
        // 1) Essaye de marquer vendu si colonne statut existe
        if (article.hasStatut) {
            String sql = "UPDATE article SET statut='vendu' WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, articleId);
                ps.executeUpdate();
                return;
            }
        }

        // 2) Sinon décrémente quantite si colonne quantite existe
        if (article.hasQuantite) {
            String sql = "UPDATE article SET quantite = quantite - ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, quantiteDemandee);
                ps.setInt(2, articleId);
                ps.executeUpdate();
                return;
            }
        }

        // 3) Sinon: impossible de marquer vendu côté DB (table article sans champ stock/statut)
        throw new IllegalStateException("La table article ne contient ni 'statut' ni 'quantite' pour marquer l'article comme vendu.");
    }

    private void tryRestockArticle(int articleId, int qte) throws SQLException {
        // Best effort: tenter quantite += qte, sinon statut = 'disponible'
        try {
            String sql = "UPDATE article SET quantite = quantite + ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, qte);
                ps.setInt(2, articleId);
                ps.executeUpdate();
                return;
            }
        } catch (SQLException ignored) {
        }
        try {
            String sql = "UPDATE article SET statut = 'disponible' WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, articleId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
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
                if (keys.next()) return keys.getInt(1);
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
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'ID de la commande.");
    }

    private void insertLigneCommandeArticle(int commandeId, int articleId, int quantite, double prixUnitaire) throws SQLException {
        String sql = "INSERT INTO ligne_commande (commande_id, article_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            ps.setInt(2, articleId);
            ps.setInt(3, quantite);
            ps.setDouble(4, prixUnitaire);
            ps.executeUpdate();
        }
    }

    private static Set<String> getColumns(ResultSetMetaData meta) throws SQLException {
        Set<String> cols = new HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            cols.add(meta.getColumnLabel(i).toLowerCase());
        }
        return cols;
    }

    private static String safeGetString(ResultSet rs, String col, Set<String> cols) throws SQLException {
        if (!cols.contains(col.toLowerCase())) return null;
        return rs.getString(col);
    }

    private static Double safeGetDouble(ResultSet rs, String col, Set<String> cols) throws SQLException {
        if (!cols.contains(col.toLowerCase())) return null;
        return rs.getDouble(col);
    }

    private static Integer safeGetInt(ResultSet rs, String col, Set<String> cols) throws SQLException {
        if (!cols.contains(col.toLowerCase())) return null;
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private record ArticleRow(double prix, boolean hasStatut, String statut, boolean hasQuantite, int quantite) {
    }

    public static class ArticleApercu {
        private final int id;
        private final String titre;
        private final double prix;
        private final String image;

        public ArticleApercu(int id, String titre, double prix, String image) {
            this.id = id;
            this.titre = titre;
            this.prix = prix;
            this.image = image;
        }

        public int getId() {
            return id;
        }

        public String getTitre() {
            return titre;
        }

        public double getPrix() {
            return prix;
        }

        public String getImage() {
            return image;
        }
    }
}

