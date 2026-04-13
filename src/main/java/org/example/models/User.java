package org.example.models;

import java.time.LocalDateTime;

/**
 * Entité centrale User : authentification, rôles, soft-delete, reset mot de passe, OAuth.
 */
public class User {
    public static final String ROLE_CLIENT = "CLIENT";
    public static final String ROLE_ARTISANT = "ARTISANT";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_LIVREUR = "LIVREUR";

    public static final String STATUT_ACTIF = "actif";
    public static final String STATUT_INACTIF = "inactif";

    public static final String SEXE_HOMME = "HOMME";
    public static final String SEXE_FEMME = "FEMME";
    public static final String SEXE_AUTRE = "AUTRE";

    private int id;
    private String nom;
    private String prenom;
    private String email;
    /** Mot de passe hashé (BCrypt) — ne jamais stocker le clair en base. */
    private String motDePasseHash;
    private String telephone;
    private String role;
    private String statut;
    private LocalDateTime deletedAt;

    private String resetTokenHash;
    private LocalDateTime resetTokenCreatedAt;
    private LocalDateTime resetTokenExpiresAt;
    private String resetTokenRequestIp;

    private String oauthProvider;
    private String oauthProviderId;

    private String sexe;
    private String avatar;

    public User() {
        this.statut = STATUT_ACTIF;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String resetTokenHash) { this.resetTokenHash = resetTokenHash; }
    public LocalDateTime getResetTokenCreatedAt() { return resetTokenCreatedAt; }
    public void setResetTokenCreatedAt(LocalDateTime resetTokenCreatedAt) { this.resetTokenCreatedAt = resetTokenCreatedAt; }
    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }
    public String getResetTokenRequestIp() { return resetTokenRequestIp; }
    public void setResetTokenRequestIp(String resetTokenRequestIp) { this.resetTokenRequestIp = resetTokenRequestIp; }
    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
    public String getOauthProviderId() { return oauthProviderId; }
    public void setOauthProviderId(String oauthProviderId) { this.oauthProviderId = oauthProviderId; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Vérification des permissions (équivalent des checks Symfony dans les controllers). */
    public boolean hasRole(String roleConstant) {
        return roleConstant != null && roleConstant.equals(this.role);
    }

    public boolean isClient() { return ROLE_CLIENT.equals(role); }
    public boolean isArtisant() { return ROLE_ARTISANT.equals(role); }
    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isLivreur() { return ROLE_LIVREUR.equals(role); }
}
