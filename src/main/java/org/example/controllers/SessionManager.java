package org.example.controllers;

public class SessionManager {

    public static Utilisateur utilisateurConnecte = null;

    public static class Utilisateur {
        public int id;
        public String nom;
        public String prenom;
        public String email;
        public String role;

        public Utilisateur(int id, String nom, String prenom, String email, String role) {
            this.id = id;
            this.nom = nom;
            this.prenom = prenom;
            this.email = email;
            this.role = role;
        }
    }
}

