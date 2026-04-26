package org.example.services;

import org.example.models.Article;
import org.example.models.Commentaire;
import org.example.models.Role;
import org.example.models.User;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ServiceCommentaire {
    private final CommentaireRepository repository;
    private final BadWordService badWordService;

    public ServiceCommentaire() {
        this(new CommentaireRepository(), new BadWordService());
    }

    public ServiceCommentaire(CommentaireRepository repository) {
        this(repository, new BadWordService());
    }

    public ServiceCommentaire(CommentaireRepository repository, BadWordService badWordService) {
        this.repository = repository;
        this.badWordService = badWordService;
    }

    public void ajouter(Commentaire commentaire) throws SQLException {
        publier(commentaire, commentaire == null ? null : commentaire.getAuteur());
    }

    public void publier(Commentaire commentaire, User currentUser) throws SQLException {
        if (currentUser == null) {
            throw new IllegalArgumentException("Vous devez être connecté pour publier un commentaire.");
        }

        if (commentaire == null) {
            throw new IllegalArgumentException("Commentaire invalide.");
        }

        if (commentaire.getAuteur() == null) {
            commentaire.setAuteur(currentUser);
        } else if (commentaire.getAuteur().getId() != currentUser.getId()) {
            throw new IllegalArgumentException("L'auteur du commentaire ne correspond pas à l'utilisateur connecté.");
        }

        validerCommentaire(commentaire);
        BadWordService.ModerationResult moderation = badWordService.verifierCommentaire(commentaire.getContenu());
        if (!moderation.isAllowed()) {
            throw new IllegalArgumentException(
                    "Commentaire contient des mots interdits: " + String.join(", ", moderation.getDetectedWords()));
        }

        if (!moderation.getFinalText().equals(commentaire.getContenu())) {
            commentaire.setContenu(moderation.getFinalText());
        }

        repository.save(commentaire);

        if (moderation.isFlagged()) {
            badWordService.enregistrerSignalement(commentaire, currentUser, moderation.getDetectedWords());
        }
    }

    // Equivalents metier des endpoints Bad Words API
    public BadWordService.ModerationResult verifierAvantPublication(String texte) throws SQLException {
        return badWordService.verifierCommentaire(texte);
    }

    public List<String> listerBadWords() throws SQLException {
        return badWordService.listerBadWords();
    }

    public void ajouterBadWord(String mot, User currentUser) throws SQLException {
        badWordService.ajouterBadWord(mot, currentUser);
    }

    public void supprimerBadWord(String mot, User currentUser) throws SQLException {
        badWordService.supprimerBadWord(mot, currentUser);
    }

    public void definirStrategieModeration(BadWordService.ModerationStrategy strategy) {
        badWordService.setStrategy(strategy);
    }

    public List<Commentaire> afficherParArticle(Article article) throws SQLException {
        return repository.findByArticle(article);
    }

    public void modifier(Commentaire commentaire, String nouveauContenu) throws SQLException {
        if (commentaire == null || commentaire.getId() <= 0) {
            throw new IllegalArgumentException("Commentaire introuvable.");
        }

        commentaire.setContenu(nouveauContenu);
        repository.updateContenu(commentaire.getId(), commentaire.getContenu());
    }

    public void modifier(Commentaire commentaire, String nouveauContenu, User currentUser) throws SQLException {
        if (commentaire == null || commentaire.getId() <= 0) {
            throw new IllegalArgumentException("Commentaire introuvable.");
        }
        if (!peutModifier(commentaire, currentUser)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce commentaire.");
        }

        commentaire.setContenu(nouveauContenu);
        repository.updateContenu(commentaire.getId(), commentaire.getContenu());
    }

    public void supprimer(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant de commentaire invalide.");
        }

        repository.deleteById(id);
    }

    public void supprimer(Commentaire commentaire, User currentUser, Article article) throws SQLException {
        if (commentaire == null || commentaire.getId() <= 0) {
            throw new IllegalArgumentException("Commentaire introuvable.");
        }

        if (!peutSupprimer(commentaire, currentUser, article)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce commentaire.");
        }

        repository.deleteById(commentaire.getId());
    }

    public boolean peutModifier(Commentaire commentaire, User currentUser) {
        return currentUser != null
                && currentUser.getRole() == Role.CLIENT
                && commentaire != null
                && commentaire.getAuteur() != null
                && commentaire.getAuteur().getId() == currentUser.getId();
    }

    public boolean peutSupprimer(Commentaire commentaire, User currentUser, Article article) {
        if (currentUser == null || currentUser.getRole() == null || commentaire == null) {
            return false;
        }

        if (currentUser.getRole() == Role.CLIENT
                && commentaire.getAuteur() != null
                && commentaire.getAuteur().getId() == currentUser.getId()) {
            return true;
        }

        return currentUser.getRole() == Role.ARTISANT
                && article != null
                && article.getArtisanId() == currentUser.getId();
    }

    private void validerCommentaire(Commentaire commentaire) {
        if (commentaire == null) {
            throw new IllegalArgumentException("Commentaire invalide.");
        }
        if (commentaire.getArticle() == null || commentaire.getArticle().getId() <= 0) {
            throw new IllegalArgumentException("Article invalide pour le commentaire.");
        }
        if (commentaire.getAuteur() == null || commentaire.getAuteur().getId() <= 0) {
            throw new IllegalArgumentException("Auteur invalide pour le commentaire.");
        }
        commentaire.setContenu(commentaire.getContenu());
        if (commentaire.getDatePub() == null) {
            commentaire.setDatePub(new Timestamp(System.currentTimeMillis()));
        }
    }
}

