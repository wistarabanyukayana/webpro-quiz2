package id.its.tracker.database;

import id.its.tracker.App;
import id.its.tracker.service.Passwords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

final class DemoAccountSeeder {

    private record Entry(int monthsAgo, int day, String type, String category, long amount, String notes, String time) {
    }

    private static final Entry[] DEMO_ENTRIES = {
        new Entry(2, 1, "income", "Salary", 7_500_000, "Monthly salary", "08:00"),
        new Entry(2, 3, "expense", "Bills", 850_000, "Electricity and internet", "19:30"),
        new Entry(2, 6, "expense", "Food & Drink", 420_000, "Groceries", "17:10"),
        new Entry(2, 12, "expense", "Transport", 300_000, "Fuel", "07:45"),
        new Entry(2, 18, "income", "Freelance", 1_200_000, "Logo design project", "14:00"),
        new Entry(2, 22, "expense", "Entertainment", 150_000, "Cinema", "20:15"),
        new Entry(1, 1, "income", "Salary", 7_500_000, "Monthly salary", "08:00"),
        new Entry(1, 4, "expense", "Bills", 870_000, "Electricity and internet", "19:00"),
        new Entry(1, 9, "expense", "Shopping", 1_350_000, "New running shoes", "16:20"),
        new Entry(1, 14, "expense", "Food & Drink", 510_000, "Groceries", "18:05"),
        new Entry(1, 20, "expense", "Health", 275_000, "Pharmacy", "10:30"),
        new Entry(1, 25, "income", "Investment", 650_000, "Dividend", "09:00"),
        new Entry(0, 1, "income", "Salary", 7_500_000, "Monthly salary", "08:00"),
        new Entry(0, 2, "expense", "Bills", 860_000, "Electricity and internet", "19:15"),
        new Entry(0, 3, "expense", "Education", 2_500_000, "Online course", "13:40"),
        new Entry(0, 5, "expense", "Food & Drink", 95_000, "Lunch with team", "12:30"),
        new Entry(0, 7, "income", "Gift", 500_000, "Birthday gift", "11:00"),
        new Entry(0, 8, "expense", "Transport", 120_000, "Ride hailing", "08:20"),
    };

    private DemoAccountSeeder() {
    }

    static void run(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM accounts WHERE lower(username) = 'demo' OR lower(email) = 'demo@example.com'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return;
            }
        }

        long accountId;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO accounts (username, full_name, email, password_hash, initial_balance) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, "demo");
            ps.setString(2, "Demo User");
            ps.setString(3, "demo@example.com");
            ps.setString(4, Passwords.hash("demo1234"));
            ps.setLong(5, 2_000_000);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT last_insert_rowid()"); ResultSet rs = ps.executeQuery()) {
            accountId = rs.getLong(1);
        }

        Map<String, Long> categoryIds = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories WHERE accounts_id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categoryIds.put(rs.getString("name"), rs.getLong("id"));
                }
            }
        }

        LocalDate today = App.today();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO transactions (type, amount, notes, date, time, accounts_id, categories_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (Entry e : DEMO_ENTRIES) {
                LocalDate month = today.withDayOfMonth(1).minusMonths(e.monthsAgo());
                LocalDate date = month.withDayOfMonth(Math.min(e.day(), month.lengthOfMonth()));
                if (date.isAfter(today)) {
                    date = today;
                }
                ps.setString(1, e.type());
                ps.setLong(2, e.amount());
                ps.setString(3, e.notes());
                ps.setString(4, date.toString());
                ps.setString(5, e.time() + ":00");
                ps.setLong(6, accountId);
                ps.setLong(7, categoryIds.get(e.category()));
                ps.executeUpdate();
            }
        }
    }
}
