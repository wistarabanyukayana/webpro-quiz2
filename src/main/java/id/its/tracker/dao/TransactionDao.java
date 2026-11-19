package id.its.tracker.dao;

import id.its.tracker.database.Database;
import id.its.tracker.model.CategoryTotal;
import id.its.tracker.model.Transaction;
import id.its.tracker.service.TransactionFilters;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TransactionDao {

    private static final String SELECT = """
            SELECT t.id, t.type, t.amount, t.notes, t.date, t.time, t.categories_id, c.name AS category
            FROM transactions t
            LEFT JOIN categories c ON c.id = t.categories_id AND c.deleted_at IS NULL
            """;
    private static final String NEWEST_FIRST = " ORDER BY t.date DESC, t.time DESC, t.id DESC";

    private TransactionDao() {
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(rs.getLong("id"), rs.getString("type"), rs.getLong("amount"), rs.getString("notes"),
                rs.getString("date"), rs.getString("time"), rs.getLong("categories_id"), rs.getString("category"));
    }

    private record Where(String sql, List<Object> params) {

        static Where of(long accountId, TransactionFilters f) {
            StringBuilder sql = new StringBuilder(" WHERE t.accounts_id = ?");
            List<Object> params = new ArrayList<>(List.of(accountId));
            if (f.type() != null) {
                sql.append(" AND t.type = ?");
                params.add(f.type());
            }
            if (f.from() != null) {
                sql.append(" AND t.date >= ?");
                params.add(f.from());
            }
            if (f.to() != null) {
                sql.append(" AND t.date <= ?");
                params.add(f.to());
            }
            if (f.category() != null) {
                sql.append(" AND t.categories_id = ?");
                params.add(f.category());
            }
            if (f.q() != null) {
                sql.append(" AND t.notes LIKE ?");
                params.add("%" + f.q() + "%");
            }
            return new Where(sql.toString(), params);
        }

        Object[] with(Object... more) {
            List<Object> all = new ArrayList<>(params);
            all.addAll(List.of(more));
            return all.toArray();
        }
    }

    public static List<Transaction> page(long accountId, TransactionFilters filters, int limit, long offset) {
        Where w = Where.of(accountId, filters);
        return Database.query(SELECT + w.sql() + NEWEST_FIRST + " LIMIT ? OFFSET ?", TransactionDao::map, w.with(limit, offset));
    }

    public static List<Transaction> all(long accountId, TransactionFilters filters) {
        Where w = Where.of(accountId, filters);
        return Database.query(SELECT + w.sql() + " ORDER BY t.date, t.time, t.id", TransactionDao::map, w.with());
    }

    public static long count(long accountId, TransactionFilters filters) {
        Where w = Where.of(accountId, filters);
        return Database.scalar("SELECT count(*) FROM transactions t" + w.sql(), w.with());
    }

    public static long sum(long accountId, TransactionFilters filters) {
        Where w = Where.of(accountId, filters);
        return Database.scalar("SELECT coalesce(sum(t.amount), 0) FROM transactions t" + w.sql(), w.with());
    }

    public static List<CategoryTotal> totalsByCategory(long accountId, TransactionFilters filters) {
        Where w = Where.of(accountId, filters);
        return Database.query("""
                SELECT t.type, coalesce(c.name, 'Uncategorized') AS category, sum(t.amount) AS total
                FROM transactions t
                LEFT JOIN categories c ON c.id = t.categories_id AND c.deleted_at IS NULL"""
                        + w.sql() + " GROUP BY t.type, t.categories_id ORDER BY t.type, total DESC, t.categories_id",
                rs -> new CategoryTotal(rs.getString("type"), rs.getString("category"), rs.getLong("total")), w.with());
    }

    public static Optional<Transaction> find(long accountId, long id) {
        return Database.one(SELECT + " WHERE t.accounts_id = ? AND t.id = ?", TransactionDao::map, accountId, id);
    }

    public static long create(long accountId, String type, long categoryId, long amount, String notes, String date, String time) {
        return Database.insert("""
                INSERT INTO transactions (type, amount, notes, date, time, accounts_id, categories_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", type, amount, notes, date, time, accountId, categoryId);
    }

    public static void update(long accountId, long id, String type, long categoryId, long amount, String notes, String date, String time) {
        Database.update("""
                UPDATE transactions SET type = ?, amount = ?, notes = ?, date = ?, time = ?, categories_id = ?
                WHERE id = ? AND accounts_id = ?""", type, amount, notes, date, time, categoryId, id, accountId);
    }

    public static void delete(long accountId, long id) {
        Database.update("DELETE FROM transactions WHERE id = ? AND accounts_id = ?", id, accountId);
    }
}
