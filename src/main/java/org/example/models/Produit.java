package org.example.models;

import java.sql.Timestamp;

public class Produit {
    private int id;
    private String nom; // Plastique, Papier, etc.
    private String typeMateriau; // Naturel, Durable, etc.
    private String etat; // Bon, Moyen, Mauvais
    private int quantite;
    private String origine;
    private String description;
    private String image;
    private int impactEcologique; // 0 à 100
    private int artisanId;
    private Timestamp dateCreation;

    public Produit() {}

    public Produit(String nom, String typeMateriau, String etat, int quantite, String origine, String description, String image, int impactEcologique, int artisanId) {
        this.nom = nom;
        this.typeMateriau = typeMateriau;
        this.etat = etat;
        this.quantite = quantite;
        this.origine = origine;
        this.description = description;
        this.image = image;
        this.impactEcologique = impactEcologique;
        this.artisanId = artisanId;
    }

    // Getters et Setters (Indispensables pour JavaFX)
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
    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public String getNomAffichage() {
        return nom == null || nom.isBlank() ? "Produit recyclable" : nom;
    }

    public String getDescriptionCourte() {
        if (description == null || description.isBlank()) {
            return "Aucune description fournie.";
        }
        String trimmed = description.trim();
        return trimmed.length() <= 140 ? trimmed : trimmed.substring(0, 137) + "...";
    }

    public String getImpactLabel() {
        if (impactEcologique >= 80) return "Très fort impact positif";
        if (impactEcologique >= 60) return "Impact positif élevé";
        if (impactEcologique >= 40) return "Impact positif moyen";
        return "Impact positif faible";
    }
}