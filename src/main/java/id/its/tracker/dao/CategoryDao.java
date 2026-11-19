package id.its.tracker.dao;

import id.its.tracker.database.Database;
import id.its.tracker.model.Category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class CategoryDao {

    private static final String SELECT = """
            SELECT c.id, c.name, c.type, (SELECT count(*) FROM transactions t WHERE t.categories_id = c.id) AS used
            FROM categories c
            WHERE c.accounts_id = ? AND c.deleted_at IS NULL""";

    private CategoryDao() {
    }

    private static Category map(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"), rs.getString("type"), rs.getLong("used"));
    }

    public static List<Category> list(long accountId) {
        return Database.query(SELECT + " ORDER BY c.type, c.name", CategoryDao::map, accountId);
    }

    public static List<Category> list(long accountId, String type) {
        return Database.query(SELECT + " AND c.type = ? ORDER BY c.name", CategoryDao::map, accountId, type);
    }

    public static Optional<Category> find(long accountId, long id) {
        return Database.one(SELECT + " AND c.id = ?", CategoryDao::map, accountId, id);
    }

    public static boolean nameTaken(long accountId, String name, String type, long exceptId) {
        return Database.scalar("""
                SELECT count(*) FROM categories
                WHERE accounts_id = ? AND name = ? AND type = ? AND deleted_at IS NULL AND id <> ?""",
                accountId, name, type, exceptId) > 0;
    }

    public static long create(long accountId, String name, String type) {
        return Database.insert("INSERT INTO categories (name, type, accounts_id) VALUES (?, ?, ?)", name, type, accountId);
    }

    public static void update(long accountId, long id, String name, String type) {
        Database.update("UPDATE categories SET name = ?, type = ? WHERE id = ? AND accounts_id = ?", name, type, id, accountId);
    }

    public static void softDelete(long accountId, long id) {
        Database.update("UPDATE categories SET deleted_at = datetime('now') WHERE id = ? AND accounts_id = ?", id, accountId);
    }
}
