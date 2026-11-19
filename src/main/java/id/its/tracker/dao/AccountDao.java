package id.its.tracker.dao;

import id.its.tracker.database.Database;
import id.its.tracker.model.Account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class AccountDao {

    private AccountDao() {
    }

    private static Account map(ResultSet rs) throws SQLException {
        return new Account(rs.getLong("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("password_hash"), rs.getString("last_login"),
                rs.getLong("initial_balance"), rs.getString("remember_token"), rs.getInt("session_version"));
    }

    public static Optional<Account> find(long id) {
        return Database.one("SELECT * FROM accounts WHERE id = ?", AccountDao::map, id);
    }

    public static Optional<Account> findByUsername(String username) {
        return Database.one("SELECT * FROM accounts WHERE lower(username) = lower(?)", AccountDao::map, username);
    }

    public static Optional<Account> findByEmail(String email) {
        return Database.one("SELECT * FROM accounts WHERE lower(email) = lower(?)", AccountDao::map, email);
    }

    public static boolean usernameTaken(String username, long exceptId) {
        return Database.scalar("SELECT count(*) FROM accounts WHERE lower(username) = lower(?) AND id <> ?",
                username, exceptId) > 0;
    }

    public static boolean emailTaken(String email, long exceptId) {
        return Database.scalar("SELECT count(*) FROM accounts WHERE lower(email) = lower(?) AND id <> ?",
                email, exceptId) > 0;
    }

    public static long create(String username, String fullName, String email, String passwordHash) {
        return Database.insert("INSERT INTO accounts (username, full_name, email, password_hash) VALUES (?, ?, ?, ?)",
                username, fullName, email, passwordHash);
    }

    public static void touchLastLogin(long id) {
        Database.update("UPDATE accounts SET last_login = datetime('now') WHERE id = ?", id);
    }

    public static void setRememberToken(long id, String tokenHash) {
        Database.update("UPDATE accounts SET remember_token = ? WHERE id = ?", tokenHash, id);
    }

    public static void updateProfile(long id, String username, String fullName, String email, long initialBalance) {
        Database.update("""
                UPDATE accounts SET username = ?, full_name = ?, email = ?, initial_balance = ?, updated_at = datetime('now')
                WHERE id = ?""", username, fullName, email, initialBalance, id);
    }

    public static int changePassword(long id, String passwordHash) {
        Database.update("""
                UPDATE accounts SET password_hash = ?, remember_token = NULL, session_version = session_version + 1,
                    updated_at = datetime('now')
                WHERE id = ?""", passwordHash, id);
        return find(id).map(Account::sessionVersion).orElseThrow();
    }
}
