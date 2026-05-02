package org.example.models;

import java.sql.Timestamp;

public class ReponseReclamation {
    private int id;
    private String contenu;
    private Timestamp dateReponse;
    private int reclamationId;
    private Reclamation reclamation;
    private int adminId;
    private User admin;

    // Constructeurs
    public ReponseReclamation() {
        this.dateReponse = new Timestamp(System.currentTimeMillis());
    }

    public ReponseReclamation(String contenu, int reclamationId, int adminId) {
        this();
        setContenu(contenu);
        this.reclamationId = reclamationId;
        this.adminId = adminId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        String value = contenu == null ? "" : contenu.trim();
        if (value.length() < 5 || value.length() > 2000) {
            throw new IllegalArgumentException("Le contenu doit contenir entre 5 et 2000 caractères.");
        }
        this.contenu = value;
    }

    public Timestamp getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(Timestamp dateReponse) {
        this.dateReponse = dateReponse == null ? new Timestamp(System.currentTimeMillis()) : dateReponse;
    }

    public int getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(int reclamationId) {
        this.reclamationId = reclamationId;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }

    // Méthodes utilitaires
    public String getFormattedDate() {
        if (dateReponse == null) {
            return "";
        }
        return new java.text.SimpleDateFormat("dd/MM/yyyy 'à' HH:mm").format(dateReponse);
    }
}

