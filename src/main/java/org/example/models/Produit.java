package org.example.models;

public class Produit {
    private int id;
    private String nom; // matériau
    private String typeMateriau;
    private String etat;
    private int quantite; // max 999999
    private String origine;
    private String description; // min 10 caractères
    private String image; // colonne SQL: image
    private int impactEcologique; // 0..100
    private int artisanId;
    private int userId;

    public Produit() {}

    public Produit(String nom, String typeMateriau, String etat, int quantite, String origine,
                   String description, String image, int impactEcologique, int artisanId, int userId) {
        this.nom = nom;
        this.typeMateriau = typeMateriau;
        this.etat = etat;
        this.quantite = quantite;
        this.origine = origine;
        this.description = description;
        this.image = image;
        this.impactEcologique = impactEcologique;
        this.artisanId = artisanId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTypeMateriau() { return typeMateriau; }
    public void setTypeMateriau(String typeMateriau) { this.typeMateriau = typeMateriau; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public String getOrigine() { return origine; }
    public void setOrigine(String origine) { this.origine = origine; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getImpactEcologique() { return impactEcologique; }
    public void setImpactEcologique(int impactEcologique) { this.impactEcologique = impactEcologique; }

    public int getArtisanId() { return artisanId; }
    public void setArtisanId(int artisanId) { this.artisanId = artisanId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}

