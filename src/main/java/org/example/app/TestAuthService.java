package org.example.app;

import org.example.models.User;
import org.example.services.AuthService;

public class TestAuthService {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: TestAuthService <email> <motdepasse>");
            return;
        }

        String email = args[0];
        String motdepasse = args[1];

        try {
            AuthService authService = new AuthService();
            User user = authService.login(email, motdepasse);
            System.out.println("Connexion OK: " + user.getEmail() + " | role=" + user.getRole());
        } catch (Exception e) {
            System.err.println("Connexion refusée: " + e.getMessage());
        }
    }
}

