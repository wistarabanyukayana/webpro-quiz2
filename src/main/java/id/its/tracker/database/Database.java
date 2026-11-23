package id.its.tracker.database;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Database {

    // bump when adding a migration
    static final int SCHEMA_VERSION = 3;

    private static volatile String url;

    private Database() {
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static final class DbException extends RuntimeException {
        DbException(SQLException cause) {
            super(cause.getMessage(), cause);
        }

        public boolean isUniqueViolation() {
            return getMessage() != null && getMessage().contains("UNIQUE constraint failed");
        }
    }

    public static void init(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create the database folder for " + file, e);
        }
        url = "jdbc:sqlite:" + file.toAbsolutePath();
    }

    public static Connection connect() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5000);
        return config.createConnection(url);
    }

    public static void bootstrap() {
        try (Connection c = connect()) {
            try (Statement s = c.createStatement()) {
                s.execute("PRAGMA journal_mode = WAL");
            }
            int version;
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA user_version")) {
                version = rs.getInt(1);
            }
            if (version < SCHEMA_VERSION) {
                c.setAutoCommit(false);
                if (version < 1) {
                    runScript(c, "database/schema.sql");
                    runScript(c, "database/extra.sql");
                }
                if (version < 2) {
                    runScript(c, "database/budgets.sql");
                }
                if (version < 3) {
                    try (Statement s = c.createStatement()) {
                        s.execute("CREATE INDEX IF NOT EXISTS ix_transactions_category ON transactions (accounts_id, categories_id, date)");
                    }
                }
                try (Statement s = c.createStatement()) {
                    s.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                }
                c.commit();
                c.setAutoCommit(true);
            }
            DatabaseSeeder.run(c);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    static void runScript(Connection c, String resource) throws SQLException {
        String script;
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
        try (Statement s = c.createStatement()) {
            for (String sql : statements(script)) {
                s.execute(sql);
            }
        }
    }

    // trigger bodies contain ; so a trigger ends at its END; line
    static List<String> statements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inTrigger = false;
        for (String line : script.split("\\R")) {
            String t = line.strip();
            if (t.startsWith("--") || (t.isEmpty() && current.isEmpty())) {
                continue;
            }
            if (t.toUpperCase().startsWith("CREATE TRIGGER")) {
                inTrigger = true;
            }
            current.append(line).append('\n');
            boolean ends = inTrigger ? t.toUpperCase().matches("END;+") : t.endsWith(";");
            if (ends) {
                String sql = current.toString().strip().replaceAll(";+$", "");
                if (!sql.isBlank()) {
                    out.add(sql);
                }
                current.setLength(0);
                inTrigger = false;
            }
        }
        return out;
    }

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            List<T> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapper.map(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public static long scalar(String sql, Object... params) {
        return one(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    public static int update(String sql, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static long insert(String sql, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = prepare(c, sql, params)) {
            ps.executeUpdate();
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    static PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }
}
