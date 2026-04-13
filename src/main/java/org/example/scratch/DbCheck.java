package org.example.scratch;

import org.example.utils.MyDatabase;
import java.sql.*;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            
            System.out.println("--- USERS ---");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM user")) {
                while (rs.next()) System.out.println("User ID: " + rs.getInt("id"));
            } catch (Exception e) { System.out.println("Table 'user' non trouvée ou vide."); }

            System.out.println("\n--- RESERVATIONS ---");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, user_id, evenement_id FROM reservation")) {
                while (rs.next()) {
                    System.out.println("Res ID: " + rs.getInt("id") + " | User ID: " + rs.getInt("user_id") + " | Evt ID: " + rs.getInt("evenement_id"));
                }
            } catch (Exception e) { System.out.println("Table 'reservation' non trouvée ou vide."); }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
