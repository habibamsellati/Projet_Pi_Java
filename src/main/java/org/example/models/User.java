package org.example.models;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getNomComplet() {
        String fullName = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return fullName.isBlank() ? email : fullName;
    }
}

