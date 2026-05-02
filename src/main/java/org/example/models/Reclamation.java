package org.example.models;

import java.sql.Timestamp;

public class Reclamation {
    private int id;
    private String titre;
    private String description;
    private String imageUrl;
    private String statut; // en_attente, en_cours, resolu, rejete
    private Timestamp dateCreation;
    private int clientId;
    private User client;

    public enum StatutReclamation {
        EN_ATTENTE("en_attente"),
        EN_COURS("en_cours"),
        RESOLU("resolu"),
        REJETE("rejete");

        private final String value;

        StatutReclamation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static StatutReclamation fromString(String value) {
            for (StatutReclamation statut : StatutReclamation.values()) {
                if (statut.value.equalsIgnoreCase(value)) {
                    return statut;
                }
            }
            return EN_ATTENTE;
        }
    }

    // Constructeurs
    public Reclamation() {
        this.dateCreation = new Timestamp(System.currentTimeMillis());
        this.statut = StatutReclamation.EN_ATTENTE.getValue();
    }

    public Reclamation(String titre, String description, int clientId) {
        this();
        setTitre(titre);
        setDescription(description);
        this.clientId = clientId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        String value = titre == null ? "" : titre.trim();
        if (value.length() < 3 || value.length() > 255) {
            throw new IllegalArgumentException("Ce champ doit être rempli (entre 3 et 255 caractères)");
        }
        this.titre = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && !description.trim().isEmpty()) {
            String value = description.trim();
            if (value.length() < 10) {
                throw new IllegalArgumentException("Au moins 10 caractères");
            }
            if (value.length() > 2000) {
                throw new IllegalArgumentException("La description ne peut pas dépasser 2000 caractères");
            }
            this.description = value;
        } else {
            this.description = null;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String lower = imageUrl.toLowerCase();
            boolean isImage = lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".png")
                    || lower.endsWith(".gif")
                    || lower.endsWith(".webp")
                    || lower.contains(".jpg?")
                    || lower.contains(".jpeg?")
                    || lower.contains(".png?")
                    || lower.contains(".gif?")
                    || lower.contains(".webp?");
            if (!isImage) {
                throw new IllegalArgumentException("Le fichier image est invalide.");
            }
            this.imageUrl = imageUrl;
        } else {
            this.imageUrl = null;
        }
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = StatutReclamation.fromString(statut).getValue();
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation == null ? new Timestamp(System.currentTimeMillis()) : dateCreation;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }

    // Méthodes utilitaires
    public String getStatutForDisplay() {
        return statut.substring(0, 1).toUpperCase() + statut.substring(1).replace("_", " ");
    }

    public String getFormattedDate() {
        if (dateCreation == null) {
            return "";
        }
        return new java.text.SimpleDateFormat("dd/MM/yyyy 'à' HH:mm").format(dateCreation);
    }
}

