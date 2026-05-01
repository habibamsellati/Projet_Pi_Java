package org.example.services;

import org.example.models.Article;
import org.example.models.Commande;
import org.example.models.User;
import org.example.utils.MyDatabase;
import org.example.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ServiceCommande {
    private final Connection connection;
    private final PersonalizedMessageService messageService;
    private final EmailService emailService;

    private static final Set<String> STATUTS_AUTORISES = Set.of("en_attente", "confirmee", "livree", "annulee");
    private static final Set<String> MODES_PAIEMENT_AUTORISES = Set.of(
            "carte", "espèces", "virement", "livraison"
    );
    private static final Pattern TELEPHONE_TUNISIEN = Pattern.compile("^[2-9]\\d{7}$");

    public ServiceCommande() {
        connection = MyDatabase.getInstance().getConnection();
        this.messageService = new PersonalizedMessageService();
        this.emailService = new EmailService();
        try {
            ensureSchema();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation du schema commande: " + e.getMessage());
        }
    }

    private void ensureSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE commande ADD COLUMN IF NOT EXISTS message_personnalise LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL");
            st.execute("ALTER TABLE commande ADD COLUMN IF NOT EXISTS ai_generated BOOLEAN NOT NULL DEFAULT false");
        }
    }

    public void ajouter(Commande commande) throws SQLException {
        preparerCommande(commande);
        validerCommande(commande);

        generarMessagePersonnalise(commande);

        String sql = "INSERT INTO commande (numero, datecommande, statut, total, adresselivraison, telephone, modepaiement, client_id, message_personnalise, ai_generated) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, commande.getNumero());
            ps.setString(2, commande.getStatut());
            ps.setDouble(3, commande.getTotal());
            ps.setString(4, commande.getAdresseLivraison());
            ps.setString(5, commande.getTelephone());
            ps.setString(6, commande.getModePaiement());
            if (commande.getClientId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, commande.getClientId());
            }
            ps.setString(8, commande.getMessagePersonnalise());
            ps.setBoolean(9, commande.isAiGenerated());
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    commande.setId(generatedKeys.getInt(1));
                }
            }
        }

        // Sauvegarder les articles dans commande_article
        if (commande.getId() > 0 && commande.getArticles() != null && !commande.getArticles().isEmpty()) {
            String sqlArticle = "INSERT IGNORE INTO commande_article (commande_id, article_id) VALUES (?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sqlArticle)) {
                for (Article article : commande.getArticles()) {
                    if (article != null && article.getId() > 0) {
                        ps.setInt(1, commande.getId());
                        ps.setInt(2, article.getId());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        }

        envoyerEmailConfirmation(commande);
    }

    private void envoyerEmailConfirmation(Commande commande) {
        if (commande == null || commande.getId() <= 0) {
            return;
        }
        envoyerEmailCommande(commande);
    }

    private void envoyerEmailCommande(Commande commande) {
        try {
            // Priorité 1 : email passé directement depuis le controller (client connecté)
            String emailClient = commande.getEmailClient();

            // Priorité 2 : session courante
            if (emailClient == null || emailClient.isBlank()) {
                User currentUser = SessionManager.getCurrentUser();
                if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
                    emailClient = currentUser.getEmail().trim();
                }
            }

            // Priorité 3 : base de données
            if ((emailClient == null || emailClient.isBlank()) && commande.getClientId() != null && commande.getClientId() > 0) {
                emailClient = obtenirEmailDuClient(commande.getClientId());
            }

            if (emailClient == null || emailClient.isBlank()) {
                System.out.println("[ServiceCommande] Email client introuvable pour commande " + commande.getNumero());
                return;
            }

            System.out.println("[ServiceCommande] Envoi email confirmation -> " + emailClient);
            emailService.envoyerConfirmationCommande(commande, emailClient);

        } catch (Exception e) {
            System.err.println("[ServiceCommande] Echec envoi email: " + e.getMessage());
        }
    }

    private String obtenirEmailDuClient(int clientId) throws SQLException {
        String sql = "SELECT email FROM `user` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        return null;
    }

    public List<Commande> afficherParClient(int clientId) throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT * FROM commande WHERE client_id = ? ORDER BY datecommande DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commande commande = new Commande();
                    commande.setId(rs.getInt("id"));
                    commande.setNumero(rs.getString("numero"));
                    commande.setDateCommande(rs.getTimestamp("datecommande"));
                    commande.setStatut(rs.getString("statut"));
                    Object total = rs.getObject("total");
                    commande.setTotal(total == null ? null : ((Number) total).doubleValue());
                    commande.setAdresseLivraison(rs.getString("adresselivraison"));
                    commande.setTelephone(rs.getString("telephone"));
                    commande.setModePaiement(rs.getString("modepaiement"));
                    Object clientIdObj = rs.getObject("client_id");
                    commande.setClientId(clientIdObj == null ? null : ((Number) clientIdObj).intValue());
                    commandes.add(commande);
                }
            }
        }
        for (Commande c : commandes) {
            chargerArticlesCommande(c);
        }
        return commandes;
    }

    private void chargerArticlesCommande(Commande commande) throws SQLException {
        String sql = "SELECT a.id, a.titre, a.contenu, a.date, a.categorie, a.prix, a.image, a.artisan_id " +
                     "FROM article a JOIN commande_article ca ON a.id = ca.article_id " +
                     "WHERE ca.commande_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, commande.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Article a = new Article();
                    a.setId(rs.getInt("id"));
                    a.setTitre(rs.getString("titre"));
                    a.setContenu(rs.getString("contenu"));
                    a.setDatePublication(rs.getTimestamp("date"));
                    a.setCategorie(rs.getString("categorie"));
                    Object prix = rs.getObject("prix");
                    a.setPrix(prix == null ? null : ((Number) prix).doubleValue());
                    a.setImageUrl(rs.getString("image"));
                    a.setArtisanId(rs.getInt("artisan_id"));
                    commande.addArticle(a);
                }
            }
        }
    }

    public void modifier(Commande commande) throws SQLException {
        preparerCommande(commande);
        validerCommande(commande);

        String sql = "UPDATE commande SET statut=?, total=?, adresselivraison=?, telephone=?, modepaiement=?, client_id=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, commande.getStatut());
            ps.setDouble(2, commande.getTotal());
            ps.setString(3, commande.getAdresseLivraison());
            ps.setString(4, commande.getTelephone());
            ps.setString(5, commande.getModePaiement());
            if (commande.getClientId() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, commande.getClientId());
            }
            ps.setInt(7, commande.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM commande WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void annulerCommande(int id) throws SQLException {
        String sql = "UPDATE commande SET statut='annulee' WHERE id=? AND statut <> 'livree'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Passe le statut de la commande à "confirmee" (validée par l'admin).
     */
    public void validerCommande(int id) throws SQLException {
        String sql = "UPDATE commande SET statut='confirmee' WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Passe le statut de la commande à "annulee" (invalidée par l'admin).
     */
    public void invaliderCommande(int id) throws SQLException {
        String sql = "UPDATE commande SET statut='annulee' WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Retourne toutes les commandes avec filtrage optionnel par mot-clé et statut,
     * avec le nom complet du client (JOIN sur user).
     */
    public List<Commande> afficherAvecFiltres(String motCle, String statut) throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient " +
                "FROM commande c LEFT JOIN `user` u ON c.client_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (motCle != null && !motCle.isBlank()) {
            sql.append(" AND (c.numero LIKE ? OR c.adresselivraison LIKE ?)");
            String pattern = "%" + motCle.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (statut != null && !statut.isBlank() && !statut.equals("Tous")) {
            sql.append(" AND c.statut = ?");
            params.add(statut);
        }
        sql.append(" ORDER BY c.datecommande DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commande commande = mapRow(rs);
                    commande.setNomClient(rs.getString("nomClient"));
                    commandes.add(commande);
                }
            }
        }
        return commandes;
    }

    /**
     * Retourne uniquement les commandes avec statut "confirmee", triées par date desc.
     */
    public List<Commande> afficherValidees() throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT c.*, CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient " +
                     "FROM commande c LEFT JOIN `user` u ON c.client_id = u.id " +
                     "WHERE c.statut = 'confirmee' ORDER BY c.datecommande DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Commande commande = mapRow(rs);
                commande.setNomClient(rs.getString("nomClient"));
                commandes.add(commande);
            }
        }
        return commandes;
    }

    /**
     * Nombre total de commandes.
     */
    public int countTotal() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM commande");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Répartition par statut : retourne un tableau de paires [statut, count].
     */
    public List<String[]> getStatsByStatut() throws SQLException {
        List<String[]> stats = new ArrayList<>();
        String sql = "SELECT statut, COUNT(*) AS cnt FROM commande GROUP BY statut";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.add(new String[]{rs.getString("statut"), String.valueOf(rs.getInt("cnt"))});
            }
        }
        return stats;
    }

    /**
     * Chiffre d'affaires total (somme des totaux des commandes validées/confirmees).
     */
    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total),0) FROM commande WHERE statut='confirmee'";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private Commande mapRow(ResultSet rs) throws SQLException {
        Commande commande = new Commande();
        commande.setId(rs.getInt("id"));
        commande.setNumero(rs.getString("numero"));
        commande.setDateCommande(rs.getTimestamp("datecommande"));
        commande.setStatut(rs.getString("statut"));
        Object total = rs.getObject("total");
        commande.setTotal(total == null ? null : ((Number) total).doubleValue());
        commande.setAdresseLivraison(rs.getString("adresselivraison"));
        commande.setTelephone(rs.getString("telephone"));
        commande.setModePaiement(rs.getString("modepaiement"));
        Object clientId = rs.getObject("client_id");
        commande.setClientId(clientId == null ? null : ((Number) clientId).intValue());
        commande.setMessagePersonnalise(rs.getString("message_personnalise"));
        try {
            commande.setAiGenerated(rs.getBoolean("ai_generated"));
        } catch (SQLException ignored) {
            commande.setAiGenerated(false);
        }
        return commande;
    }

    public List<Commande> afficher() throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT c.*, CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient " +
                     "FROM commande c LEFT JOIN `user` u ON c.client_id = u.id ORDER BY c.datecommande DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Commande commande = mapRow(rs);
                commande.setNomClient(rs.getString("nomClient"));
                commandes.add(commande);
            }
        }
        return commandes;
    }

    public Commande trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM commande WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private void preparerCommande(Commande commande) {
        if (commande == null) {
            return;
        }
        if (commande.getNumero() == null || commande.getNumero().isBlank()) {
            commande.setNumero(Commande.generateNumero());
        }
        if ((commande.getTotal() == null || commande.getTotal() <= 0)
                && commande.getArticles() != null
                && !commande.getArticles().isEmpty()) {
            commande.setTotal(commande.calculateTotal());
        }
        if (commande.getStatut() == null || commande.getStatut().isBlank()) {
            commande.setStatut("en_attente");
        }
        if (commande.getTelephone() != null && commande.getTelephone().isBlank()) {
            commande.setTelephone(null);
        }
        if (commande.getTelephone() != null) {
            commande.setTelephone(normaliserTelephone(commande.getTelephone()));
        }
        if (commande.getModePaiement() != null) {
            commande.setModePaiement(normaliserModePaiement(commande.getModePaiement()));
        }
    }

    private void generarMessagePersonnalise(Commande commande) {
        if (commande == null) {
            return;
        }

        try {
            String nomClient = resolveNomClient(commande);

            List<String> titresArticles = new ArrayList<>();
            for (Article article : commande.getArticles()) {
                if (article != null && article.getTitre() != null && !article.getTitre().isBlank()) {
                    titresArticles.add(article.getTitre());
                }
            }

            PersonalizedMessageService.MessageGenerationResult result = messageService.generateOrderConfirmationMessage(
                    nomClient,
                    titresArticles,
                    commande.getTotal() != null ? commande.getTotal() : 0.0,
                    commande.getNumero()
            );

            commande.setNomClient(nomClient);
            commande.setMessagePersonnalise(result.message());
            commande.setAiGenerated(result.aiGenerated());
        } catch (Exception e) {
            System.err.println("Erreur lors de la generation du message personnalise: " + e.getMessage());
            commande.setMessagePersonnalise("Merci pour votre commande !");
            commande.setAiGenerated(false);
        }
    }

    private String resolveNomClient(Commande commande) {
        if (commande.getNomClient() != null && !commande.getNomClient().isBlank()) {
            return commande.getNomClient().trim();
        }
        if (commande.getClientId() != null && commande.getClientId() > 0) {
            try {
                String nom = obtenirNomDuClient(commande.getClientId());
                if (nom != null && !nom.isBlank()) {
                    return nom.trim();
                }
            } catch (SQLException e) {
                System.err.println("Impossible de recuperer le nom du client: " + e.getMessage());
            }
        }
        return "Client";
    }

    private String obtenirNomDuClient(int clientId) throws SQLException {
        String sql = "SELECT CONCAT(COALESCE(prenom,''), ' ', COALESCE(nom,'')) AS nomComplet FROM `user` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nomComplet");
                }
            }
        }
        return null;
    }

    private void validerCommande(Commande commande) {
        if (commande == null) {
            throw new IllegalArgumentException("Commande invalide.");
        }

        if (commande.getTotal() == null || commande.getTotal() <= 0) {
            throw new IllegalArgumentException("Le total doit être supérieur à zéro");
        }

        String adresse = commande.getAdresseLivraison() == null ? "" : commande.getAdresseLivraison().trim();
        if (adresse.length() < 10) {
            throw new IllegalArgumentException("L'adresse doit contenir au moins 10 caractères");
        }
        if (adresse.length() > 255) {
            throw new IllegalArgumentException("L'adresse ne peut pas dépasser 255 caractères");
        }

        String telephone = commande.getTelephone();
        if (telephone != null && !telephone.isBlank() && !TELEPHONE_TUNISIEN.matcher(telephone).matches()) {
            throw new IllegalArgumentException("Numéro tunisien valide requis (ex: 20 123 456, 20123456, +21620123456)");
        }

        if (commande.getStatut() == null || !STATUTS_AUTORISES.contains(commande.getStatut())) {
            throw new IllegalArgumentException("Statut invalide");
        }

        if (commande.getModePaiement() == null || !MODES_PAIEMENT_AUTORISES.contains(commande.getModePaiement())) {
            throw new IllegalArgumentException("Mode de paiement invalide");
        }
    }

    private String normaliserTelephone(String telephone) {
        if (telephone == null) {
            return null;
        }

        String normalise = telephone.trim().replaceAll("[\\s\\-().]", "");
        if (normalise.isBlank()) {
            return null;
        }

        String numeroLocal = normalise;
        if (numeroLocal.startsWith("+216")) {
            numeroLocal = numeroLocal.substring(4);
        } else if (numeroLocal.startsWith("00216")) {
            numeroLocal = numeroLocal.substring(5);
        } else if (numeroLocal.startsWith("216") && numeroLocal.length() == 11) {
            numeroLocal = numeroLocal.substring(3);
        }

        return numeroLocal;
    }

    private String normaliserModePaiement(String modePaiement) {
        if (modePaiement == null) {
            return null;
        }

        String v = modePaiement.trim().toLowerCase();
        if (v.isBlank()) {
            return null;
        }

        // Harmonise les variantes saisies côté UI/DB
        if (v.equals("especes")) {
            return "espèces";
        }
        if (v.contains("livraison") || v.equals("cod") || v.equals("cash_on_delivery")) {
            return "livraison";
        }
        return v;
    }
}

