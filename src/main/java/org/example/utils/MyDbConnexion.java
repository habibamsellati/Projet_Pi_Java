package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDbConnexion {
    private String USER_NAME = "root";
    private String PASSWORD = "";
    private String URL = "jdbc:mysql://localhost:3306/piprojet1";

    private Connection cnx;

    //2ND STEP : CREATE A VAR THAT HAS THE SAME TYPE AS THIS CLASS
    private static MyDbConnexion instance;

    //1ST STEP : MAKE THE CONSTRUCTOR PRIVATE
    private   MyDbConnexion() {

        try {
            cnx = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
            System.out.println("Connexion OK");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
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
}
