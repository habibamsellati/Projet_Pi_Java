package org.example.utils;

import org.example.models.User;

/**
 * Stockage de session pour l'utilisateur actuellement connecté.
 */
public class SessionStore {
    private static User currentUser = null;

    public static void login(User user) {
        currentUser = user;
        System.out.println("DEBUG: Utilisateur connecté : " + user.getEmail() + " [Role: " + user.getRole() + "]");
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isConnected() {
        return currentUser != null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int getUserId() {
        return (currentUser != null) ? currentUser.getId() : -1;
    }

    public static String getUserRole() {
        return (currentUser != null) ? currentUser.getRole() : "";
    }

    public static boolean isArtisan() {
        return currentUser != null && User.ROLE_ARTISANT.equals(currentUser.getRole());
    }

    public static boolean isClient() {
        return currentUser != null && User.ROLE_CLIENT.equals(currentUser.getRole());
    }

    public static boolean isAdmin() {
        return currentUser != null && User.ROLE_ADMIN.equals(currentUser.getRole());
    }
}
