package org.example.models;

import java.time.LocalDateTime;

public class Reclamation {
    private int id;
    private int userId;
    private String titre;
    // Colonne volontairement "descripition" pour rester compatible avec le schéma existant.
    private String descripition;
    private String image;
    private LocalDateTime dateCreation;
    private String statut;

    public Reclamation() {
    }

    public Reclamation(int userId, String titre, String descripition, String image, String statut) {
        this.userId = userId;
        this.titre = titre;
        this.descripition = descripition;
        this.image = image;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescripition() { return descripition; }
    public void setDescripition(String descripition) { this.descripition = descripition; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}

