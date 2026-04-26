package tn.esprit.utils;

public class EmailConfirmationSession {

    private static String email;
    private static String code;
    private static long createdAt;

    public static void start(String userEmail, String generatedCode) {
        email = userEmail;
        code = generatedCode;
        createdAt = System.currentTimeMillis();
    }

    public static boolean verify(String inputCode) {
        if (isExpired()) return false;
        return code != null && code.equals(inputCode);
    }

    public static boolean isExpired() {
        return (System.currentTimeMillis() - createdAt) > (10 * 60 * 1000);
    }

    public static String getEmail() {
        return email;
    }

    public static void clear() {
        email = null;
        code = null;
        createdAt = 0;
    }
}