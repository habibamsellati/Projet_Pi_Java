package org.example.models;

import java.sql.Timestamp;

public class Proposition {
    private int id;
    private int produitId;
    private int userId;
    private String titre;
    private String description;
    private double prixPropose;
    private String telephone;
    private String image;
    private String statut; // "en attente", "acceptée", "refusée"
    private String nomProduit;
    private Timestamp dateCreation;

    public Proposition() { this.statut = "en attente"; }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrixPropose() { return prixPropose; }
    public void setPrixPropose(double prixPropose) { this.prixPropose = prixPropose; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public String getTitreAffichage() {
        return titre == null || titre.isBlank() ? "Proposition sans titre" : titre;
    }

    public String getDescriptionCourte() {
        if (description == null || description.isBlank()) {
            return "Aucune description fournie.";
        }
        String trimmed = description.trim();
        return trimmed.length() <= 140 ? trimmed : trimmed.substring(0, 137) + "...";
    }
}