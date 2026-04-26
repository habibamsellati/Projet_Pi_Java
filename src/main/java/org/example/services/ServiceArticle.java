package org.example.services;

import org.example.models.Article;
import org.example.utils.MyDatabase;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ServiceArticle {
    private Connection connection;
    private static final Set<String> CATEGORIES_AUTORISEES = Set.of(
            "Artisanat", "Décoration", "Textile", "Céramique", "Autres"
    );

    public ServiceArticle() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // 1. AJOUTER UN ARTICLE
    public void ajouter(Article a) throws SQLException {
        validerArticle(a);

        // On force la date courante pour eviter l'erreur "Field 'date' doesn't have a default value"
        String sql = "INSERT INTO article (titre, contenu, date, categorie, prix, image, likes, artisan_id, user_id) VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getTitre());
            ps.setString(2, a.getContenu());
            ps.setString(3, a.getCategorie());
            if (a.getPrix() == null) {
                ps.setNull(4, Types.DECIMAL);
            } else {
                ps.setDouble(4, a.getPrix());
            }
            ps.setString(5, a.getImageUrl());
            ps.setInt(6, 0);

            // On utilise l'id de l'artisan, sinon une valeur de test
            int idArtisan = (a.getArtisanId() == 0) ? 3 : a.getArtisanId();
            ps.setInt(7, idArtisan);
            ps.setInt(8, idArtisan); // user_id par defaut

            ps.executeUpdate();
        }
    }

    // 2. MODIFIER UN ARTICLE
    public void modifier(Article a) throws SQLException {
        validerArticle(a);

        String sql = "UPDATE article SET titre=?, contenu=?, categorie=?, prix=?, image=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getTitre());
            ps.setString(2, a.getContenu());
            ps.setString(3, a.getCategorie());
            if (a.getPrix() == null) {
                ps.setNull(4, Types.DECIMAL);
            } else {
                ps.setDouble(4, a.getPrix());
            }
            ps.setString(5, a.getImageUrl());
            ps.setInt(6, a.getId());
            ps.executeUpdate();
        }
    }

    // 3. SUPPRIMER UN ARTICLE
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM article WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // 4. AFFICHER LES ARTICLES
    public List<Article> afficher() throws SQLException {
        List<Article> articles = new ArrayList<>();
        String sql = "SELECT * FROM article ORDER BY date DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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
                a.setLikes(rs.getInt("likes"));
                // dislikes : colonne ajoutée dynamiquement, lecture défensive
                try { a.setDislikes(rs.getInt("dislikes")); } catch (Exception ignored) {}
                articles.add(a);
            }
        }
        return articles;
    }

    private void validerArticle(Article a) {
        if (a == null) {
            throw new IllegalArgumentException("Article invalide.");
        }

        String titre = a.getTitre() == null ? "" : a.getTitre().trim();
        if (titre.isEmpty() || titre.length() < 3) {
            throw new IllegalArgumentException("Ce champ doit contenir au moins 3 caractères.");
        }
        if (titre.length() > 255) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 255 caractères.");
        }

        String contenu = a.getContenu() == null ? "" : a.getContenu().trim();
        if (contenu.isEmpty() || contenu.length() < 10) {
            throw new IllegalArgumentException("Ce champ doit contenir au moins 10 caractères.");
        }

        if (a.getPrix() != null && a.getPrix() < 0) {
            throw new IllegalArgumentException("Le prix doit être positif ou nul.");
        }

        String categorie = a.getCategorie();
        if (categorie != null && !categorie.isBlank() && !CATEGORIES_AUTORISEES.contains(categorie)) {
            throw new IllegalArgumentException("Catégorie invalide.");
        }

        String imageNormalisee = normaliserImageUrl(a.getImageUrl());
        a.setImageUrl(imageNormalisee);
        if (imageNormalisee != null && !isImageReferenceValide(imageNormalisee)) {
            throw new IllegalArgumentException("URL image invalide. Utilisez http(s), file:/ ou un chemin image valide.");
        }
    }

    private String normaliserImageUrl(String image) {
        if (image == null) {
            return null;
        }
        String trimmed = image.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isImageReferenceValide(String image) {
        String lower = image.toLowerCase(Locale.ROOT);

        boolean hasImageExtension = lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.contains(".jpg?")
                || lower.contains(".jpeg?")
                || lower.contains(".png?")
                || lower.contains(".gif?")
                || lower.contains(".webp?");

        if (hasImageExtension) {
            return true;
        }

        // Accepte les URLs web/file valides meme sans extension explicite (CDN, signed URLs, etc.).
        try {
            URI uri = new URI(image);
            String scheme = uri.getScheme();
            if (scheme != null) {
                return "http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)
                        || "file".equalsIgnoreCase(scheme);
            }
        } catch (URISyntaxException ignored) {
            // On tente ensuite une validation souple pour les chemins locaux.
        }

        // Chemin local Windows/Unix saisi manuellement.
        return image.matches("^[A-Za-z]:\\\\.*") || image.startsWith("/");
    }
}