package org.example.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String nom;
    private String artisan;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private int capacite;
    private String typeArt;
    private String theme;
    private BigDecimal prix;
    private String image;
    private String statut;
    private LocalDateTime createdAt;

    public Evenement() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getArtisan() { return artisan; }
    public void setArtisan(String artisan) { this.artisan = artisan; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getTypeArt() { return typeArt; }
    public void setTypeArt(String typeArt) { this.typeArt = typeArt; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        if (nom != null && dateDebut != null) {
            return nom + " (le " + dateDebut.toLocalDate().toString() + ")";
        }
        return nom != null ? nom : "Événement sans nom";
    }
}
