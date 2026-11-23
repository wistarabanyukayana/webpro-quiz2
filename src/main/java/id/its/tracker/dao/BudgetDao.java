package id.its.tracker.dao;

import id.its.tracker.database.Database;
import id.its.tracker.model.Budget;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class BudgetDao {

    private static final String SELECT = """
            SELECT b.id, b.categories_id, c.name, b.month, b.amount,
                   (SELECT coalesce(sum(t.amount), 0) FROM transactions t
                    WHERE t.accounts_id = b.accounts_id AND t.categories_id = b.categories_id
                      AND t.type = 'expense' AND t.date >= b.month || '-01' AND t.date <= b.month || '-31') AS spent
            FROM budgets b
            JOIN categories c ON c.id = b.categories_id
            WHERE b.accounts_id = ?""";

    private BudgetDao() {
    }

    private static Budget map(ResultSet rs) throws SQLException {
        return new Budget(rs.getLong("id"), rs.getLong("categories_id"), rs.getString("name"), rs.getString("month"),
                rs.getLong("amount"), rs.getLong("spent"));
    }

    public static List<Budget> list(long accountId, String month) {
        return Database.query(SELECT + " AND b.month = ? ORDER BY c.name", BudgetDao::map, accountId, month);
    }

    public static Optional<Budget> find(long accountId, long id) {
        return Database.one(SELECT + " AND b.id = ?", BudgetDao::map, accountId, id);
    }

    public static boolean taken(long accountId, long categoryId, String month, long exceptId) {
        return Database.scalar("""
                SELECT count(*) FROM budgets WHERE accounts_id = ? AND categories_id = ? AND month = ? AND id <> ?""",
                accountId, categoryId, month, exceptId) > 0;
    }

    public static boolean usesCategory(long categoryId) {
        return Database.scalar("SELECT count(*) FROM budgets WHERE categories_id = ?", categoryId) > 0;
    }

    public static long create(long accountId, long categoryId, String month, long amount) {
        return Database.insert("INSERT INTO budgets (month, amount, accounts_id, categories_id) VALUES (?, ?, ?, ?)",
                month, amount, accountId, categoryId);
    }

    public static void update(long accountId, long id, long categoryId, String month, long amount) {
        Database.update("""
                UPDATE budgets SET categories_id = ?, month = ?, amount = ?, updated_at = datetime('now')
                WHERE id = ? AND accounts_id = ?""", categoryId, month, amount, id, accountId);
    }

    public static void delete(long accountId, long id) {
        Database.update("DELETE FROM budgets WHERE id = ? AND accounts_id = ?", id, accountId);
    }
}
