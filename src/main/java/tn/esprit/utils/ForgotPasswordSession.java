package tn.esprit.utils;

public class ForgotPasswordSession {

    private static String email;
    private static String token;
    private static long createdAt;

    public static void start(String userEmail, String generatedToken) {
        email = userEmail;
        token = generatedToken;
        createdAt = System.currentTimeMillis();
    }

    public static String getEmail() {
        return email;
    }

    public static String getToken() {
        return token;
    }

    public static boolean hasPendingRequest() {
        return email != null && token != null;
    }

    public static boolean isExpired() {
        return createdAt == 0 || (System.currentTimeMillis() - createdAt) > 10 * 60 * 1000;
    }

    public static void clear() {
        email = null;
        token = null;
        createdAt = 0;
    }
}