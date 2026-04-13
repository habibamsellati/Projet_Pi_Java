package org.example.app;

import org.example.utils.MyDatabase;
import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        // Test de récupération de la connexion
        Connection cn = MyDatabase.getInstance().getConnection();

        if (cn != null) {
            System.out.println("Bravo ! TestDB confirme que la connexion est active.");
        } else {
            System.err.println("Échec du test. Vérifiez votre serveur MySQL.");
        }
    }
}