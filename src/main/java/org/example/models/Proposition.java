package org.example.models;

public class Proposition {
    private int id;
    private int produitId;
    private int userId;
    private String titre;
    private String description;
    private double prixPropose;
    private String telephone;
    private String image; // colonne SQL: image
    private String statut; // 'en attente' par défaut

    public Proposition() {
    }

    public Proposition(int produitId, int userId, String titre, String description, double prixPropose, String telephone, String image, String statut) {
        this.produitId = produitId;
        this.userId = userId;
        this.titre = titre;
        this.description = description;
        this.prixPropose = prixPropose;
        this.telephone = telephone;
        this.image = image;
        this.statut = statut;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrixPropose() {
        return prixPropose;
    }

    public void setPrixPropose(double prixPropose) {
        this.prixPropose = prixPropose;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}

