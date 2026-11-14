package com.aetheria.rewards.data;

import java.sql.*;
import java.util.UUID;

/** SQLite o MySQL/MariaDB. Una fila por jugador con el YAML serializado (compatible con varios servidores; gana la ultima escritura). */
public final class SqlStore implements DataStore {
    private final String url, user, pass; private final boolean mysql;

    public SqlStore(String url, String user, String pass, boolean mysql) throws SQLException {
        this.url = url; this.user = user; this.pass = pass; this.mysql = mysql;
        try { Class.forName(mysql ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) {}
        try (Connection c = open(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS aetheria_players (uuid VARCHAR(36) PRIMARY KEY, data MEDIUMTEXT, updated BIGINT)");
        }
    }

    private Connection open() throws SQLException { return mysql ? DriverManager.getConnection(url, user, pass) : DriverManager.getConnection(url); }

    @Override public synchronized String load(UUID id) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT data FROM aetheria_players WHERE uuid=?")) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    @Override public synchronized void save(UUID id, String yaml) throws SQLException {
        String sql = mysql
            ? "INSERT INTO aetheria_players(uuid,data,updated) VALUES(?,?,?) ON DUPLICATE KEY UPDATE data=VALUES(data), updated=VALUES(updated)"
            : "INSERT OR REPLACE INTO aetheria_players(uuid,data,updated) VALUES(?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.toString()); ps.setString(2, yaml); ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    @Override public String name() { return mysql ? "MySQL/MariaDB" : "SQLite"; }
}
