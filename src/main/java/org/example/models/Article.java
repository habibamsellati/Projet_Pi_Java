package org.example.models;

import java.sql.Timestamp;

public class Article {
    private int id;
    private String titre;
    private String contenu;
    private Timestamp datePublication;
    private String categorie; // Artisanat, Décoration, etc.
    private Double prix;
    private String imageUrl;
    private int artisanId;
    private int likes;
    private int dislikes;

    // Constructeurs
    public Article() {}

    public Article(String titre, String contenu, String categorie, Double prix, String imageUrl, int artisanId) {
        this.titre = titre;
        this.contenu = contenu;
        this.categorie = categorie;
        this.prix = prix;
        this.imageUrl = imageUrl;
        this.artisanId = artisanId;
    }

    // Getters et Setters (indispensables pour JavaFX)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public Timestamp getDatePublication() { return datePublication; }
    public void setDatePublication(Timestamp datePublication) { this.datePublication = datePublication; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getArtisanId() { return artisanId; }
    public void setArtisanId(int artisanId) { this.artisanId = artisanId; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = Math.max(0, likes); }
    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = Math.max(0, dislikes); }
}
