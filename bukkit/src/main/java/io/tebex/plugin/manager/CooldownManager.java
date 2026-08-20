package io.tebex.plugin.manager;

import java.io.File;
import java.sql.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CooldownManager {
    private Connection connection;
    private final Logger logger;

    public CooldownManager(File dataFolder, Logger logger) {
        this.logger = logger;
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS cooldowns (" +
                    "player_name TEXT NOT NULL, " +
                    "package_id INTEGER NOT NULL, " +
                    "claimed_at INTEGER NOT NULL, " +
                    "cooldown_seconds INTEGER NOT NULL, " +
                    "PRIMARY KEY (player_name, package_id))"
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize cooldown database", e);
            connection = null;
        }
    }

    public synchronized void recordClaim(String playerName, int packageId, int cooldownSeconds) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO cooldowns (player_name, package_id, claimed_at, cooldown_seconds) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, playerName.toLowerCase(Locale.ENGLISH));
            ps.setInt(2, packageId);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, cooldownSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to record cooldown claim", e);
        }
    }

    public synchronized boolean isOnCooldown(String playerName, int packageId, int cooldownSeconds) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT claimed_at FROM cooldowns WHERE player_name = ? AND package_id = ?")) {
            ps.setString(1, playerName.toLowerCase(Locale.ENGLISH));
            ps.setInt(2, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long claimedAt = rs.getLong("claimed_at");
                    long elapsed = System.currentTimeMillis() - claimedAt;
                    return elapsed < (long) cooldownSeconds * 1000L;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to check cooldown", e);
        }
        return false;
    }

    public synchronized void cleanup() {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM cooldowns WHERE claimed_at + (cooldown_seconds * 1000) < ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to cleanup expired cooldowns", e);
        }
    }

    public synchronized void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close cooldown database", e);
        }
        connection = null;
    }
}
