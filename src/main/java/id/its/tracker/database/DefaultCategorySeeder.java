package id.its.tracker.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class DefaultCategorySeeder {

    private static final String[][] DEFAULT_CATEGORIES = {
        {"Salary", "income"}, {"Freelance", "income"}, {"Investment", "income"}, {"Gift", "income"},
        {"Other Income", "income"},
        {"Food & Drink", "expense"}, {"Transport", "expense"}, {"Shopping", "expense"}, {"Bills", "expense"},
        {"Health", "expense"}, {"Entertainment", "expense"}, {"Education", "expense"}, {"Other Expense", "expense"},
    };

    private DefaultCategorySeeder() {
    }

    static void run(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO default_categories (name, type) VALUES (?, ?)")) {
            for (String[] row : DEFAULT_CATEGORIES) {
                ps.setString(1, row[0]);
                ps.setString(2, row[1]);
                ps.executeUpdate();
            }
        }
    }
}
