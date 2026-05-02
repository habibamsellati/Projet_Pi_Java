package org.example.models;

public enum Role {
    ARTISANT,
    CLIENT,
    ADMIN,
    LIVREUR,
    RESPONSABLE,
    UNKNOWN;

    public static Role fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}

