package org.example.models;

/**
 * Représente un artisan recommandé avec son score et sa justification.
 */
public class ArtisanRecommendation implements Comparable<ArtisanRecommendation> {
    private int artisanId;
    private String artisanNom;
    private String artisanPrenom;
    private String email;
    private int score;
    private String matchReason;
    private String specialties; // ex: "Menuiserie, Ébénisterie"
    private int realisationsReussies;
    private int propositionsEnvoyees;

    public ArtisanRecommendation(int artisanId, String artisanNom, String artisanPrenom, 
                                  String email, int score, String matchReason,
                                  String specialties, int realisationsReussies, int propositionsEnvoyees) {
        this.artisanId = artisanId;
        this.artisanNom = artisanNom;
        this.artisanPrenom = artisanPrenom;
        this.email = email;
        this.score = score;
        this.matchReason = matchReason;
        this.specialties = specialties;
        this.realisationsReussies = realisationsReussies;
        this.propositionsEnvoyees = propositionsEnvoyees;
    }

    // Getters
    public int getArtisanId() { return artisanId; }
    public String getArtisanNom() { return artisanNom; }
    public String getArtisanPrenom() { return artisanPrenom; }
    public String getEmail() { return email; }
    public int getScore() { return score; }
    public String getMatchReason() { return matchReason; }
    public String getSpecialties() { return specialties; }
    public int getRealisationsReussies() { return realisationsReussies; }
    public int getPropositionsEnvoyees() { return propositionsEnvoyees; }

    public String getNomComplet() {
        return (artisanPrenom != null ? artisanPrenom + " " : "") + (artisanNom != null ? artisanNom : "");
    }

    @Override
    public int compareTo(ArtisanRecommendation other) {
        // Tri décroissant par score (meilleur d'abord)
        return Integer.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("[%d pts] %s - %s", score, getNomComplet(), matchReason);
    }
}

