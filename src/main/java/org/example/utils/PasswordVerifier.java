package org.example.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class PasswordVerifier {

    private PasswordVerifier() {}

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null) return false;
        if (storedHash == null || storedHash.isBlank()) {
            // Compte sans mot de passe (hash vide) → accepter si mot de passe vide
            return rawPassword.isBlank();
        }

        // Comparaison directe (mot de passe en clair)
        if (storedHash.equals(rawPassword)) return true;

        // Hash bcrypt ($2a$ ou $2y$) → utiliser PHP si disponible
        if (storedHash.startsWith("$2")) {
            return verifierAvecPhp(rawPassword, storedHash);
        }

        // Hash MD5 ou SHA (legacy)
        if (storedHash.length() == 32 || storedHash.length() == 40 || storedHash.length() == 64) {
            return verifierHashSimple(rawPassword, storedHash);
        }

        return false;
    }

    private static boolean verifierAvecPhp(String rawPassword, String storedHash) {
        ProcessBuilder pb = new ProcessBuilder(
                resolvePhpCommand(),
                "-r",
                "$password=$argv[1]; $hash=$argv[2]; exit(password_verify($password,$hash)?0:1);",
                "--",
                rawPassword,
                storedHash
        );
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) { process.destroyForcibly(); return false; }
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (r.readLine() != null) { /* vider le flux */ }
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            System.err.println("[PasswordVerifier] PHP indisponible: " + e.getMessage());
            return false;
        }
    }

    private static boolean verifierHashSimple(String rawPassword, String storedHash) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolvePhpCommand() {
        String configured = System.getenv("PHP_BINARY");
        return configured != null && !configured.isBlank() ? configured : "php";
    }
}
