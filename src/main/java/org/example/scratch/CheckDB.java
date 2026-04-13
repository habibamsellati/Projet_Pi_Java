package org.example.scratch;

import org.example.utils.MyDatabase;
import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = MyDatabase.getInstance().getConnection();
            System.out.println("Checking DB...");
            
            // Check users
            String[] tables = {"user", "users"};
            for (String table : tables) {
                try {
                    st = conn.createStatement();
                    rs = st.executeQuery("SELECT id FROM " + table + " LIMIT 1");
                    if (rs.next()) {
                        System.out.println("VALID_ID_FOUND_IN_" + table.toUpperCase() + ": " + rs.getInt("id"));
                    }
                } catch (Exception e) {
                    System.out.println("Table " + table + " not found or empty.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (st != null) st.close(); } catch (Exception e) {}
        }
    }
}
