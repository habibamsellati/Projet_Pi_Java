package org.example.services;

import org.example.models.Proposition;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceProposition {
    public static final String STATUT_EN_ATTENTE = "en attente";
    public static final String STATUT_ACCEPTEE = "acceptée";
    public static final String STATUT_REFUSEE = "refusée";

    private final Connection connection;

    public ServiceProposition() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
    }

    // CRUD: AJOUTER
    public void ajouter(Proposition p) throws SQLException {
        if (p.getProduitId() <= 0) {
            throw new IllegalArgumentException("produit_id doit être un ID auto-increment valide (> 0).");
        }
        if (!produitExiste(p.getProduitId())) {
            throw new IllegalArgumentException("Produit introuvable pour produit_id=" + p.getProduitId());
        }

        String sql = "INSERT INTO proposition (produit_id, user_id, titre, description, prix_propose, telephone, image, statut) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, p.getProduitId());
            ps.setInt(2, (p.getUserId() == 0) ? 3 : p.getUserId());
            ps.setString(3, p.getTitre());
            ps.setString(4, p.getDescription());
            ps.setDouble(5, p.getPrixPropose());
            ps.setString(6, p.getTelephone());
            ps.setString(7, p.getImage());
            ps.setString(8, (p.getStatut() == null || p.getStatut().isBlank()) ? STATUT_EN_ATTENTE : p.getStatut());
            ps.executeUpdate();
        }
    }

    // CRUD: MODIFIER (interdit si acceptée)
    public void modifier(Proposition p) throws SQLException {
        if (p.getId() <= 0) {
            throw new IllegalArgumentException("Modification impossible: utilisez l'ID de proposition (auto-increment).");
        }
        if (isAcceptee(p.getId())) {
            throw new IllegalStateException("Une proposition acceptée ne peut plus être modifiable.");
        }

        String sql = "UPDATE proposition SET titre=?, description=?, prix_propose=?, telephone=?, image=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrixPropose());
            ps.setString(4, p.getTelephone());
            ps.setString(5, p.getImage());
            ps.setString(6, p.getStatut());
            ps.setInt(7, p.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Aucune proposition trouvée avec id=" + p.getId());
            }
        }
    }

    // CRUD: SUPPRIMER
    public void supprimer(int propositionId) throws SQLException {
        if (propositionId <= 0) {
            throw new IllegalArgumentException("Suppression impossible: utilisez l'ID de proposition (auto-increment).");
        }
        String sql = "DELETE FROM proposition WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, propositionId);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new IllegalArgumentException("Aucune proposition trouvée avec id=" + propositionId);
            }
        }
    }

    // CRUD: AFFICHER
    public List<Proposition> afficher() throws SQLException {
        List<Proposition> propositions = new ArrayList<>();
        String sql = "SELECT * FROM proposition";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Proposition p = new Proposition();
                p.setId(rs.getInt("id"));
                p.setProduitId(rs.getInt("produit_id"));
                p.setUserId(rs.getInt("user_id"));
                p.setTitre(rs.getString("titre"));
                p.setDescription(rs.getString("description"));
                p.setPrixPropose(rs.getDouble("prix_propose"));
                p.setTelephone(rs.getString("telephone"));
                p.setImage(rs.getString("image"));
                p.setStatut(rs.getString("statut"));
                propositions.add(p);
            }
        }
        return propositions;
    }

    // Règle: % de différence entre prix proposé et prix original produit
    // Retour: +X% si prix proposé > prix original, -X% sinon
    public double calculerDifferencePourcentage(int produitId, double prixPropose) throws SQLException {
        double prixOriginal = getProduitPrixOriginal(produitId);
        if (prixOriginal <= 0) {
            throw new IllegalStateException("Prix original du produit invalide (<= 0).");
        }
        return ((prixPropose - prixOriginal) / prixOriginal) * 100.0;
    }

    // Lecture du prix original du produit (nécessite une colonne 'prix' dans la table produit)
    public double getProduitPrixOriginal(int produitId) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Produit introuvable (id=" + produitId + ").");
                }

                Set<String> cols = getColumns(rs.getMetaData());
                if (!cols.contains("prix")) {
                    throw new IllegalStateException(
                            "La table 'produit' ne contient pas la colonne 'prix'. " +
                                    "Ajoutez une colonne 'prix' (ex: DOUBLE) pour calculer la différence en pourcentage."
                    );
                }
                return rs.getDouble("prix");
            }
        }
    }

    public ProduitInfo getProduitInfo(int produitId) throws SQLException {
        String sql = "SELECT id, nom, type_materiau, etat, quantite, origine, image, impact_ecologique FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ProduitInfo(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("type_materiau"),
                        rs.getString("etat"),
                        rs.getInt("quantite"),
                        rs.getString("origine"),
                        rs.getString("image"),
                        rs.getInt("impact_ecologique")
                );
            }
        }
    }

    private boolean isAcceptee(int propositionId) throws SQLException {
        String sql = "SELECT statut FROM proposition WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, propositionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String statut = rs.getString("statut");
                return statut != null && statut.equalsIgnoreCase(STATUT_ACCEPTEE);
            }
        }
    }

    private boolean produitExiste(int produitId) throws SQLException {
        String sql = "SELECT id FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Set<String> getColumns(ResultSetMetaData meta) throws SQLException {
        Set<String> cols = new HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            cols.add(meta.getColumnLabel(i).toLowerCase());
        }
        return cols;
    }

    public static class ProduitInfo {
        private final int id;
        private final String nom;
        private final String typeMateriau;
        private final String etat;
        private final int quantite;
        private final String origine;
        private final String image;
        private final int impact;

        public ProduitInfo(int id, String nom, String typeMateriau, String etat, int quantite, String origine, String image, int impact) {
            this.id = id;
            this.nom = nom;
            this.typeMateriau = typeMateriau;
            this.etat = etat;
            this.quantite = quantite;
            this.origine = origine;
            this.image = image;
            this.impact = impact;
        }

        public int getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public String getTypeMateriau() {
            return typeMateriau;
        }

        public String getEtat() {
            return etat;
        }

        public int getQuantite() {
            return quantite;
        }

        public String getOrigine() {
            return origine;
        }

        public String getImage() {
            return image;
        }

        public int getImpact() {
            return impact;
        }
    }
}

