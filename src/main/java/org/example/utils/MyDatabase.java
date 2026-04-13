package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String host = getConfig("DB_HOST", "localhost");
    private final String port = getConfig("DB_PORT", "3306");
    private final String dbName = getConfig("DB_NAME", "piprojet1");
    private final String USER = getConfig("DB_USER", "root");
    private final String PWD = getConfig("DB_PWD", "");
    private final String URL = "jdbc:mysql://" + host + ":" + port + "/" + dbName
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PWD);
            System.out.println("Connexion MySQL réussie : " + host + ":" + port + " / " + dbName);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Impossible de se connecter à MySQL (" + host + ":" + port + " / base " + dbName + "). " +
                    "Vérifiez que le serveur est démarré, que le port est correct, et que USER/PWD sont valides. " +
                    "Cause JDBC: " + e.getMessage(),
                    e
            );
        }
    }

    private String getConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}