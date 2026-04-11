package tn.esprit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
}
