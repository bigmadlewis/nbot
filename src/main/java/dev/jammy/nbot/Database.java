package dev.jammy.nbot;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:activity.db";

    public static void init() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS activity (user_id TEXT PRIMARY KEY, last_active INTEGER)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateActivity(String userId) {
        String sql = "INSERT INTO activity(user_id, last_active) VALUES(?, ?) " +
                "ON CONFLICT(user_id) DO UPDATE SET last_active = excluded.last_active";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}