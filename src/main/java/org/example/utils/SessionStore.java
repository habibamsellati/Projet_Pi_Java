package org.example.utils;

import org.example.models.Role;
import org.example.models.User;

/**
 * Session facade utilisée par le module Evenement/Reservation.
 */
public final class SessionStore {
    private SessionStore() {
    }

    public static void login(User user) {
        SessionManager.login(user);
    }

    public static void logout() {
        SessionManager.logout();
    }

    public static boolean isConnected() {
        return SessionManager.isLoggedIn();
    }

    public static User getCurrentUser() {
        return SessionManager.getCurrentUser();
    }

    public static int getUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : -1;
    }

    public static String getUserRole() {
        User user = getCurrentUser();
        return user != null && user.getRole() != null ? user.getRole().name() : "";
    }

    public static boolean isArtisan() {
        return SessionManager.hasRole(Role.ARTISANT);
    }

    public static boolean isClient() {
        return SessionManager.hasRole(Role.CLIENT);
    }

    public static boolean isAdmin() {
        return SessionManager.hasRole(Role.ADMIN);
    }
}

