package org.example.models;

public class LigneCommandeArticle {
    private int articleId;
    private int quantite;

    public LigneCommandeArticle() {
    }

    public LigneCommandeArticle(int articleId, int quantite) {
        this.articleId = articleId;
        this.quantite = quantite;
    }

    public int getArticleId() { return articleId; }
    public void setArticleId(int articleId) { this.articleId = articleId; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
}
