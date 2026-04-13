package org.example.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Équivalent côté Java de PasswordAuthenticatedUserInterface : stockage hash BCrypt.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}
