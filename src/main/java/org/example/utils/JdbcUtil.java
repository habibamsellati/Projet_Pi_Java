package org.example.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public final class JdbcUtil {
    private JdbcUtil() {
    }

    public static boolean hasColumn(ResultSet rs, String columnLabel) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (columnLabel.equalsIgnoreCase(md.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    /** Colonne absente ou table incompatible (repli sur un autre INSERT). */
    public static boolean isMissingColumnError(SQLException ex) {
        String m = ex.getMessage();
        return m != null && (m.contains("Unknown column") || m.contains("doesn't exist"));
    }
}
