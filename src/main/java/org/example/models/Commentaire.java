package org.example.models;

import java.time.LocalDateTime;

public class Commentaire {
    private int id;
    private int articleId;
    private int userId;
    private Integer parentId;
    private String contenu;
    private LocalDateTime datePub;
    private int likes;
    private int dislikes;

    public Commentaire() {
    }

    /** Ancien écran (note / étoiles) — contenu doit respecter les validations du service. */
    public Commentaire(int articleId, int userId, String contenu, int note) {
        this.articleId = articleId;
        this.userId = userId;
        this.contenu = contenu;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getArticleId() { return articleId; }
    public void setArticleId(int articleId) { this.articleId = articleId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDatePub() { return datePub; }
    public void setDatePub(LocalDateTime datePub) { this.datePub = datePub; }

    /** Alias pour l’ancien code (FXML / contrôleurs). */
    public LocalDateTime getDatePublication() { return datePub; }
    public void setDatePublication(LocalDateTime datePub) { this.datePub = datePub; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }

    /** Compat ancien module (note 1–5), optionnel si colonne absente en base */
    private Integer note;

    public Integer getNote() { return note; }
    public void setNote(Integer note) { this.note = note; }

    /** Affichage étoiles (0 si absent). */
    public int getNoteStars() {
        return note == null ? 0 : note;
    }
}
