package org.example.models;
import java.time.LocalDateTime;


public class livraison {

    private Integer id;
    private LocalDateTime dateLivraison;
    private String addressLivraison;
    private String statutLivraison;
    private String noteLivreur;
    private Integer livreurId;
    private Double lat;
    private Double lng;
    private Integer commandeId;

    // Constructeur vide (nécessaire pour beaucoup de frameworks comme Hibernate/Spring)
    public livraison() {
    }

    // Constructeur complet
    public livraison(Integer id, LocalDateTime dateLivraison, String addressLivraison,
                     String statutLivraison, String noteLivreur, Integer livreurId,
                     Double lat, Double lng, Integer commandeId) {
        this.id = id;
        this.dateLivraison = dateLivraison;
        this.addressLivraison = addressLivraison;
        this.statutLivraison = statutLivraison;
        this.noteLivreur = noteLivreur;
        this.livreurId = livreurId;
        this.lat = lat;
        this.lng = lng;
        this.commandeId = commandeId;
    }

    // --- Getters et Setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }

    public String getAddressLivraison() { return addressLivraison; }
    public void setAddressLivraison(String addressLivraison) { this.addressLivraison = addressLivraison; }

    public String getStatutLivraison() { return statutLivraison; }
    public void setStatutLivraison(String statutLivraison) { this.statutLivraison = statutLivraison; }

    public String getNoteLivreur() { return noteLivreur; }
    public void setNoteLivreur(String noteLivreur) { this.noteLivreur = noteLivreur; }

    public Integer getLivreurId() { return livreurId; }
    public void setLivreurId(Integer livreurId) { this.livreurId = livreurId; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Integer getCommandeId() { return commandeId; }
    public void setCommandeId(Integer commandeId) { this.commandeId = commandeId; }

    // Optionnel : Méthode toString pour faciliter le debug
    @Override
    public String toString() {
        return "Livraison{" +
                "id=" + id +
                ", statut='" + statutLivraison + '\'' +
                ", adresse='" + addressLivraison + '\'' +
                '}';
    }
}
