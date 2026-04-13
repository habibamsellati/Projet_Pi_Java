package org.example.scratch;

import org.example.utils.MyDatabase;
import java.sql.*;

public class CheckUsers {
    public static void main(String[] args) {
        try (Connection conn = MyDatabase.getInstance().getConnection()) {
            System.out.println("Checking tables...");
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    System.out.println("Table: " + rs.getString("TABLE_NAME"));
                }
            }

            String tableName = "user"; // Try singular first based on SQL error
            System.out.println("\nQuerying table: " + tableName);
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, email FROM " + tableName + " LIMIT 5")) {
                while (rs.next()) {
                    System.out.println("Found user: ID=" + rs.getInt("id") + ", Email=" + rs.getString("email"));
                }
            } catch (SQLException e) {
                System.out.println("Table 'user' failed, trying 'users'...");
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id, email FROM users LIMIT 5")) {
                    while (rs.next()) {
                        System.out.println("Found user: ID=" + rs.getInt("id") + ", Email=" + rs.getString("email"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
