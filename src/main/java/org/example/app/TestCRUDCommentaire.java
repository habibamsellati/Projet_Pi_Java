package org.example.app;

import org.example.models.Article;
import org.example.models.Commentaire;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceArticle;
import org.example.services.ServiceCommentaire;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TestCRUDCommentaire {
    public static void main(String[] args) {
        ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
        ServiceArticle serviceArticle = new ServiceArticle();

        try {
            Article article = chargerPremierArticle(serviceArticle);
            User auteur = chargerPremierUtilisateur();

            if (article == null) {
                System.out.println("ℹ️ Aucun article disponible. Créez d'abord un article pour tester les commentaires.");
                return;
            }
            if (auteur == null) {
                System.out.println("ℹ️ Aucun utilisateur disponible. Créez d'abord un utilisateur pour tester les commentaires.");
                return;
            }

            Commentaire commentaire = new Commentaire();
            commentaire.setContenu("Commentaire de test automatique");
            commentaire.setArticle(article);
            commentaire.setAuteur(auteur);
            serviceCommentaire.ajouter(commentaire);
            System.out.println("✅ Ajout commentaire OK, id=" + commentaire.getId());

            List<Commentaire> commentaires = serviceCommentaire.afficherParArticle(article);
            System.out.println("📋 Nombre de commentaires pour l'article " + article.getId() + " : " + commentaires.size());

            serviceCommentaire.modifier(commentaire, "Commentaire modifié automatiquement");
            System.out.println("✏️ Modification commentaire OK");

            serviceCommentaire.supprimer(commentaire.getId());
            System.out.println("🗑️ Suppression commentaire OK");
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
        }
    }

    private static Article chargerPremierArticle(ServiceArticle serviceArticle) throws SQLException {
        List<Article> articles = serviceArticle.afficher();
        return articles.isEmpty() ? null : articles.get(0);
    }

    private static User chargerPremierUtilisateur() throws SQLException {
        Connection connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            return null;
        }

        String sql = "SELECT id, nom, prenom, email, role FROM `user` LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }

            User user = new User();
            user.setId(rs.getInt("id"));
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setEmail(rs.getString("email"));
            user.setRole(Role.fromDatabaseValue(rs.getString("role")));
            return user;
        }
    }
}

