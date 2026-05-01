package org.example.services;

import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Service IA de recommandation d'artisans avec scoring pondéré.
 *
 * Formule :
 *   - Acceptée / Terminée  → +10 pts (réussite)
 *   - En attente / Révision → +2 pts (proactivité)
 *   - Aucune proposition    → +5 pts (boost nouveau)
 */
public class PropositionAIService {

    public record ArtisanScore(
            int    id,
            String nom,
            String initiales,
            int    score,
            int    nbPropositions,
            int    nbReussies,
            int    nbMateriau,
            String specialites,
            String matchReason
    ) {}

    private static final int POINTS_REUSSITE    = 10;
    private static final int POINTS_PROACTIVITE = 2;
    private static final int POINTS_BOOST_NOUVEAU = 5;
    private static final int TOP_N = 5;

    // Matching matériau → spécialités
    private static final Map<String, List<String>> SPECIALITES = Map.of(
        "bois",      List.of("Menuiserie", "Ébénisterie", "Sculpture", "Bois"),
        "plastique", List.of("Upcycling", "Impression 3D", "Plastique"),
        "tissu",     List.of("Couture", "Tapisserie", "Patchwork", "Textile"),
        "metal",     List.of("Ferronnerie", "Soudure", "Métal"),
        "verre",     List.of("Vitrail", "Verre"),
        "carton",    List.of("Cartonnage", "Carton"),
        "papier",    List.of("Origami", "Papier")
    );

    private final Connection connection;

    public PropositionAIService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Retourne le top 5 artisans recommandés pour un matériau donné.
     */
    public List<ArtisanScore> recommanderArtisans(String nomMateriau) throws SQLException {
        // 1. Récupérer tous les artisans actifs
        List<int[]> artisans = new ArrayList<>(); // [id]
        String sqlArtisans = "SELECT id FROM user WHERE role = 'ARTISANT' " +
                "AND (statut IS NULL OR LOWER(statut) = 'actif') LIMIT 20";
        try (PreparedStatement ps = connection.prepareStatement(sqlArtisans);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) artisans.add(new int[]{rs.getInt("id")});
        }

        // 2. Calculer le score de chaque artisan
        List<ArtisanScore> scores = new ArrayList<>();
        for (int[] a : artisans) {
            ArtisanScore score = calculerScore(a[0], nomMateriau);
            if (score != null) scores.add(score);
        }

        // 3. Trier par score décroissant puis pertinence matière
        scores.sort(
                Comparator.comparingInt(ArtisanScore::score).reversed()
                        .thenComparing(Comparator.comparingInt(ArtisanScore::nbMateriau).reversed())
                        .thenComparing(Comparator.comparingInt(ArtisanScore::nbReussies).reversed())
        );

        // 4. Retourner le top N
        return scores.stream().limit(TOP_N).toList();
    }

    private ArtisanScore calculerScore(int artisanId, String nomMateriau) throws SQLException {
        // Récupérer infos artisan
        String sqlUser = "SELECT nom, prenom FROM user WHERE id = ?";
        String nom = "", prenom = "";
        try (PreparedStatement ps = connection.prepareStatement(sqlUser)) {
            ps.setInt(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                nom    = nvl(rs.getString("nom"));
                prenom = nvl(rs.getString("prenom"));
            }
        }

        // Compter les propositions reçues sur les produits de l'artisan
        String sqlProps = "SELECT p.statut, COUNT(*) as cnt FROM proposition p " +
                "JOIN produit pr ON p.produit_id = pr.id " +
                "WHERE pr.added_by_id = ? GROUP BY p.statut";
        int nbReussies = 0, nbProactives = 0, nbTotal = 0;
        try (PreparedStatement ps = connection.prepareStatement(sqlProps)) {
            ps.setInt(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statut = rs.getString("statut");
                    int cnt = rs.getInt("cnt");
                    nbTotal += cnt;
                    if ("acceptee".equals(statut) || "terminee".equals(statut)) nbReussies += cnt;
                    else if ("en_attente".equals(statut) || "en_revision".equals(statut)) nbProactives += cnt;
                }
            }
        }

        int nbMateriau = compterMateriau(artisanId, nomMateriau);
        String specialites = specialitesPour(nomMateriau);

        // Calcul score
        int score;
        String matchReason;
        if (nbTotal == 0) {
            score = POINTS_BOOST_NOUVEAU;
            matchReason = "Prêt pour son premier défi";
        } else {
            score = (nbReussies * POINTS_REUSSITE) + (nbProactives * POINTS_PROACTIVITE);
            if (nbReussies > 0) {
                matchReason = nbReussies + " réalisation(s) réussie(s)";
            } else {
                matchReason = nbTotal + " proposition(s) envoyée(s)";
            }

            if (nbMateriau > 0) {
                matchReason += " • " + nbMateriau + " sur ce matériau";
            }
        }

        // Initiales
        String fullName = (prenom + " " + nom).trim();
        String initiales = buildInitiales(fullName);

        return new ArtisanScore(artisanId, fullName, initiales, score, nbTotal, nbReussies, nbMateriau, specialites, matchReason);
    }

    private int compterMateriau(int artisanId, String nomMateriau) throws SQLException {
        if (nomMateriau == null || nomMateriau.isBlank()) return 0;

        String sql = "SELECT COUNT(*) FROM proposition p " +
                "JOIN produit pr ON p.produit_id = pr.id " +
                "WHERE pr.added_by_id = ? AND LOWER(pr.nom) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artisanId);
            ps.setString(2, nomMateriau.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String specialitesPour(String nomMateriau) {
        if (nomMateriau == null || nomMateriau.isBlank()) return "Polyvalent";
        List<String> values = SPECIALITES.get(nomMateriau.trim().toLowerCase());
        if (values == null || values.isEmpty()) return "Polyvalent";
        return String.join(", ", values);
    }

    /**
     * Estimation du prix basée sur le matériau, l'état et le type.
     */
    public double estimerPrix(String nomMateriau, String etat, String typeMateriau) {
        double base = switch (nvl(nomMateriau).toLowerCase()) {
            case "bois"      -> 50.0;
            case "metal"     -> 45.0;
            case "verre"     -> 35.0;
            case "plastique" -> 25.0;
            case "tissu"     -> 30.0;
            case "carton"    -> 20.0;
            case "papier"    -> 15.0;
            default          -> 30.0;
        };

        double mult = switch (nvl(etat).toLowerCase()) {
            case "excellent" -> 1.4;
            case "bon"       -> 1.2;
            case "moyen"     -> 1.0;
            default          -> 0.8;
        };

        if (nvl(typeMateriau).toLowerCase().contains("naturel") ||
            nvl(typeMateriau).toLowerCase().contains("ecolo")) {
            mult *= 1.1;
        }

        return Math.round(base * mult * 10.0) / 10.0;
    }

    private String buildInitiales(String fullName) {
        if (fullName == null || fullName.isBlank()) return "??";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String nvl(String v) { return v == null ? "" : v.trim(); }
}
