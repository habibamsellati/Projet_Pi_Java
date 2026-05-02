package org.example.utils;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public final class PasswordResetSession {

    private static String email;
    private static String code;
    private static Instant expiresAt;

    private PasswordResetSession() {
    }

    public static synchronized String start(String targetEmail) {
        email = targetEmail;
        code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        expiresAt = Instant.now().plusSeconds(10 * 60);
        return code;
    }

    public static synchronized boolean matches(String targetEmail, String submittedCode) {
        if (email == null || code == null || expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            clear();
            return false;
        }
        return email.equalsIgnoreCase(targetEmail) && code.equals(submittedCode);
    }

    public static synchronized boolean isActiveFor(String targetEmail) {
        return email != null
                && expiresAt != null
                && Instant.now().isBefore(expiresAt)
                && email.equalsIgnoreCase(targetEmail);
    }

    public static synchronized void clear() {
        email = null;
        code = null;
        expiresAt = null;
    }
}

