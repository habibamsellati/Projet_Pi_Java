package tn.esprit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MyDbConnexion {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/pi_projet?useSSL=false&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private Connection cnx;

    //2ND STEP : CREATE A VAR THAT HAS THE SAME TYPE AS THIS CLASS
    private static MyDbConnexion instance;

    //1ST STEP : MAKE THE CONSTRUCTOR PRIVATE
    private   MyDbConnexion() {
        Properties properties = loadProperties();
        String url = properties.getProperty("db.url", DEFAULT_URL);
        String userName = properties.getProperty("db.username", DEFAULT_USER);
        String password = properties.getProperty("db.password", DEFAULT_PASSWORD);

        try {
            cnx = DriverManager.getConnection(url, userName, password);
            System.out.println("Connexion OK");
        } catch (SQLException e) {
            System.err.println("Echec connexion BD: " + e.getMessage());
        }

    }

    //3RD STEP : CREATE A METHOD THAT RETURNS THE INSTANCE
    public static MyDbConnexion getInstance() {
        if (instance == null) {
            instance = new MyDbConnexion();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire db.properties: " + e.getMessage());
        }

        return properties;
    }

    public static class ModuleAccessManager {

        private static final Map<String, String[]> MODULE_CREDENTIALS = new HashMap<>();

        static {
            MODULE_CREDENTIALS.put("produits", new String[]{"produits", "produit"});
            MODULE_CREDENTIALS.put("propositions", new String[]{"propositions", "prop"});
            MODULE_CREDENTIALS.put("livraison", new String[]{"livraison", "liv"});
            MODULE_CREDENTIALS.put("reclamations", new String[]{"reclamations", "reclam"});
            MODULE_CREDENTIALS.put("articles", new String[]{"articles", "article"});
            MODULE_CREDENTIALS.put("evenements", new String[]{"evenements", "event"});
            MODULE_CREDENTIALS.put("reservations", new String[]{"reservations", "resa"});
            MODULE_CREDENTIALS.put("suivi", new String[]{"suivi", "suivi"});
        }

        public static boolean validateAccess(String moduleKey, String username, String password) {
            if (!MODULE_CREDENTIALS.containsKey(moduleKey)) {
                return false;
            }

            String[] creds = MODULE_CREDENTIALS.get(moduleKey);
            return creds[0].equals(username) && creds[1].equals(password);
        }

        public static String getDefaultUsername(String moduleKey) {
            return MODULE_CREDENTIALS.containsKey(moduleKey) ? MODULE_CREDENTIALS.get(moduleKey)[0] : "";
        }
    }
}
