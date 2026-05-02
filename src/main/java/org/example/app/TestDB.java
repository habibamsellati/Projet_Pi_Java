package org.example.app;

import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        try (Connection connection = MyDatabase.getInstance().requireConnection()) {
            System.out.println("Bravo ! TestDB confirme que la connexion est active.");
        } catch (SQLException e) {
            System.err.println("Échec du test JDBC : " + e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("Échec du test : " + e.getMessage());
        }
    }
}