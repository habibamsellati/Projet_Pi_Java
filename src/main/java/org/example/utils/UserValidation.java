package org.example.utils;

import org.example.models.User;

import java.util.regex.Pattern;

/**
 * Validations alignées sur les contraintes Symfony décrites pour l'entité User.
 */
public final class UserValidation {

    public static final int NOM_MAX = 15;
    public static final int PRENOM_MAX = 30;

    /** Lettres uniquement (y compris accents), espaces, tiret et apostrophe pour noms composés. */
    private static final Pattern NOM_PRENOM = Pattern.compile("^[\\p{L}\\s'-]+$");

    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /** Au moins une majuscule, un chiffre et un symbole spécial (non alphanumérique). */
    private static final Pattern MOT_DE_PASSE_FORT = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$"
    );

    /** Téléphone tunisien : 8 chiffres (optionnel si vide). */
    private static final Pattern TELEPHONE_TUNISIE = Pattern.compile("^[0-9]{8}$");

    private UserValidation() {
    }

    public static void validerNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        String t = nom.trim();
        if (t.length() > NOM_MAX) {
            throw new IllegalArgumentException("Le nom ne peut pas dépasser " + NOM_MAX + " caractères.");
        }
        if (!NOM_PRENOM.matcher(t).matches()) {
            throw new IllegalArgumentException("Le nom ne doit contenir que des lettres (espaces, tiret et apostrophe autorisés).");
        }
    }

    public static void validerPrenom(String prenom) {
        if (prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire.");
        }
        String t = prenom.trim();
        if (t.length() > PRENOM_MAX) {
            throw new IllegalArgumentException("Le prénom ne peut pas dépasser " + PRENOM_MAX + " caractères.");
        }
        if (!NOM_PRENOM.matcher(t).matches()) {
            throw new IllegalArgumentException("Le prénom ne doit contenir que des lettres (espaces, tiret et apostrophe autorisés).");
        }
    }

    public static void validerEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire.");
        }
        String t = email.trim();
        if (!EMAIL.matcher(t).matches()) {
            throw new IllegalArgumentException("Format d'email invalide.");
        }
    }

    private static final int MOT_DE_PASSE_MIN = 8;

    private static void validerMotDePasseClair(String motDePasseClair) {
        if (motDePasseClair == null || motDePasseClair.length() < MOT_DE_PASSE_MIN) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins " + MOT_DE_PASSE_MIN + " caractères.");
        }
        if (!MOT_DE_PASSE_FORT.matcher(motDePasseClair).matches()) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins une majuscule, un chiffre et un symbole spécial."
            );
        }
    }

    public static void validerMotDePasseClairPourInscription(String motDePasseClair) {
        validerMotDePasseClair(motDePasseClair);
    }

    public static void validerTelephoneOptionnel(String telephone) {
        if (telephone == null || telephone.isBlank()) return;
        String t = telephone.trim();
        if (!TELEPHONE_TUNISIE.matcher(t).matches()) {
            throw new IllegalArgumentException("Le téléphone doit contenir exactement 8 chiffres.");
        }
    }

    public static void validerRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Le rôle est obligatoire.");
        }
        String r = role.trim();
        if (!r.equals(User.ROLE_CLIENT) && !r.equals(User.ROLE_ARTISANT) && !r.equals(User.ROLE_ADMIN) && !r.equals(User.ROLE_LIVREUR)) {
            throw new IllegalArgumentException("Rôle invalide.");
        }
    }

    public static void validerStatut(String statut) {
        if (statut == null || statut.isBlank()) return;
        String s = statut.trim();
        if (!s.equals(User.STATUT_ACTIF) && !s.equals(User.STATUT_INACTIF)) {
            throw new IllegalArgumentException("Statut invalide (actif ou inactif).");
        }
    }

    public static void validerSexeOptionnel(String sexe) {
        if (sexe == null || sexe.isBlank()) return;
        String s = sexe.trim();
        if (!s.equals(User.SEXE_HOMME) && !s.equals(User.SEXE_FEMME) && !s.equals(User.SEXE_AUTRE)) {
            throw new IllegalArgumentException("Sexe invalide (HOMME, FEMME ou AUTRE).");
        }
    }
}
