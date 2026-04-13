package org.example.models;

import java.time.LocalDateTime;

public class Reservation {
    private int id;
    private int evenementId;
    private Integer userId;
    private LocalDateTime dateReservation;
    private int nbPlaces;
    private String statut;
    private LocalDateTime createdAt;

    public Reservation() {
    }

    public Reservation(int evenementId, Integer userId, LocalDateTime dateReservation, int nbPlaces, String statut) {
        this.evenementId = evenementId;
        this.userId = userId;
        this.dateReservation = dateReservation;
        this.nbPlaces = nbPlaces;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Réservation Événement ID: " + evenementId + " (" + nbPlaces + " place(s)) - Statut: " + statut;
    }
}
