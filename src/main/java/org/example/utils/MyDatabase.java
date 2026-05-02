package org.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MyDatabase {
     private static final String DEFAULT_URL = "jdbc:mysql://127.0.0.1:3307/piprojet1?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&sessionVariables=character_set_client=utf8mb4,character_set_connection=utf8mb4,character_set_results=utf8mb4&connectTimeout=5000&socketTimeout=5000";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PWD = "";

    private final String url;
    private final String user;
    private final String password;

    private final Connection connection;
    private Connection physicalConnection;
    private String lastConnectionErrorMessage = "Connexion à la base de données indisponible.";
    private static MyDatabase instance;

    private MyDatabase() {
        Properties properties = loadProperties();
        url = getConfig("db.url", "DB_URL", properties, DEFAULT_URL);
        user = getConfig("db.username", "DB_USERNAME", properties, DEFAULT_USER);
        password = getConfig("db.password", "DB_PASSWORD", properties, DEFAULT_PWD);
        connection = createConnectionProxy();
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        return connection;
    }

    public synchronized Connection requireConnection() {
        Connection activeConnection = tryConnect();
        if (activeConnection == null) {
            throw new IllegalStateException(lastConnectionErrorMessage);
        }
        return activeConnection;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire db.properties : " + e.getMessage());
        }

        return properties;
    }

    private String getConfig(String propertyKey, String envKey, Properties properties, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }

    private void logConnectionError(String url, String user, SQLException e) {
        System.err.println("Erreur SQL : " + e.getMessage());
        System.err.println("URL JDBC : " + url);
        System.err.println("Utilisateur : " + user);
        System.err.println("SQLState : " + e.getSQLState());
        System.err.println("Code erreur : " + e.getErrorCode());

        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        String target = extractConnectionTarget(url);
        if (message.contains("communications link failure") || message.contains("connection refused")) {
            System.err.println("Diagnostic : aucun serveur MySQL ne répond actuellement à cette adresse/ce port.");
            System.err.println("Vérifiez que MySQL/XAMPP/WAMP est démarré et qu'il écoute bien sur " + target + ".");
            lastConnectionErrorMessage = "Impossible de contacter MySQL sur " + target + ". Démarrez MySQL/XAMPP/WAMP puis réessayez.";
        } else if (message.contains("access denied")) {
            System.err.println("Diagnostic : identifiant ou mot de passe MySQL incorrect.");
            lastConnectionErrorMessage = "Accès MySQL refusé. Vérifiez le nom d'utilisateur et le mot de passe dans db.properties.";
        } else if (message.contains("unknown database")) {
            System.err.println("Diagnostic : la base de données demandée n'existe pas.");
            lastConnectionErrorMessage = "La base de données 'piprojet1' est introuvable. Créez-la ou corrigez db.properties.";
        } else {
            lastConnectionErrorMessage = "Connexion MySQL impossible : " + e.getMessage();
        }
    }

    private synchronized Connection tryConnect() {
        try {
            if (physicalConnection == null || physicalConnection.isClosed()) {
                physicalConnection = DriverManager.getConnection(url, user, password);
                lastConnectionErrorMessage = null;
                System.out.println("Connexion MySQL réussie sur " + extractConnectionTarget(url) + " (base: " + extractDatabaseName(url) + ") !");
            }
            return physicalConnection;
        } catch (SQLException e) {
            physicalConnection = null;
            logConnectionError(url, user, e);
            return null;
        }
    }

    private Connection createConnectionProxy() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if ("toString".equals(methodName)) {
                        return "MyDatabaseConnectionProxy[" + url + "]";
                    }
                    if ("hashCode".equals(methodName)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(methodName)) {
                        return proxy == args[0];
                    }
                    if ("close".equals(methodName)) {
                        closePhysicalConnection();
                        return null;
                    }
                    if ("isClosed".equals(methodName)) {
                        return physicalConnection == null || physicalConnection.isClosed();
                    }

                    Connection target = requireConnection();
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
                }
        );
    }

    private synchronized void closePhysicalConnection() throws SQLException {
        if (physicalConnection != null && !physicalConnection.isClosed()) {
            physicalConnection.close();
        }
        physicalConnection = null;
    }

    private String extractConnectionTarget(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "127.0.0.1:3307";
        }

        String sanitized = jdbcUrl.replace("jdbc:mysql://", "");
        int slashIndex = sanitized.indexOf('/');
        return slashIndex >= 0 ? sanitized.substring(0, slashIndex) : sanitized;
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "piprojet1";
        }

        String sanitized = jdbcUrl.replace("jdbc:mysql://", "");
        int slashIndex = sanitized.indexOf('/');
        if (slashIndex < 0 || slashIndex == sanitized.length() - 1) {
            return "inconnue";
        }

        String dbPart = sanitized.substring(slashIndex + 1);
        int queryIndex = dbPart.indexOf('?');
        return queryIndex >= 0 ? dbPart.substring(0, queryIndex) : dbPart;
    }
}