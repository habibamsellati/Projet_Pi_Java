package org.example;

import org.example.utils.MyDatabase;
import java.sql.*;

public class UserInspector {
    public static void main(String[] args) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            String sql = "SELECT email, role FROM users WHERE deleted_at IS NULL LIMIT 20";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println("--- LISTE DES UTILISATEURS ---");
            while (rs.next()) {
                System.out.println("Email: " + rs.getString("email") + " | Role: " + rs.getString("role"));
            }
            System.out.println("-----------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
