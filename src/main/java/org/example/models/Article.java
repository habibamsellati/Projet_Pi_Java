package org.example.models;

import java.time.LocalDateTime;

public class Article {
    private int id;
    private String titre;
    private String contenu;
    private LocalDateTime datePublication;
    private double prix;
    private String categorie;
    /** Colonne SQL : image */
    private String image;
    private int artisanId;
    private int userId;

    public Article() {
    }

    public Article(String titre, String contenu, double prix, String categorie, String image, int artisanId, int userId) {
        this.titre = titre;
        this.contenu = contenu;
        this.prix = prix;
        this.categorie = categorie;
        this.image = image;
        this.artisanId = artisanId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDateTime datePublication) { this.datePublication = datePublication; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    /** Compat ancien code */
    public String getImageUrl() { return image; }
    public void setImageUrl(String image) { this.image = image; }
    public int getArtisanId() { return artisanId; }
    public void setArtisanId(int artisanId) { this.artisanId = artisanId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() {
        return titre + " — " + String.format("%.2f", prix) + " DT";
    }
}
