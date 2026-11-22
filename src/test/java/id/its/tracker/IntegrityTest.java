package id.its.tracker;

import id.its.tracker.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrityTest {

    private static long count(String sql, Object... params) {
        return Database.scalar(sql, params);
    }

    private static int tableRows(String page) {
        return page.split("<tr class=\"border-t border-border\">").length - 1;
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "zero amount      | amount      | 0          | The amount field must be at least 1.",
        "negative amount  | amount      | -5         | The amount field must be at least 1.",
        "decimal amount   | amount      | 10.5       | The amount field must be an integer.",
        "text amount      | amount      | abc        | The amount field must be an integer.",
        "unknown type     | type        | transfer   | The selected type is invalid.",
        "bad date         | date        | 2026-13-40 | The date field must match the format Y-m-d.",
        "bad time         | time        | 25:99      | The time field must match the format H:i.",
        "missing category | category_id | ''         | The category id field is required.",
    })
    void transactionInputIsValidated(String label, String field, String value, String message) {
        String name = unique("val");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "Val");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        List<String> form = new ArrayList<>(List.of("type", "expense", "category_id", "" + category,
                "amount", "1000", "date", "2026-10-25", "time", "10:00"));
        form.set(form.indexOf(field) + 1, value == null ? "" : value);
        String page = client.follow(client.post("/transactions", form.toArray(String[]::new)));

        assertTrue(page.contains(message), label);
        assertEquals(0, count("SELECT count(*) FROM transactions WHERE accounts_id = ?", accountId));
    }

    @Test
    void aTransactionCannotUseACategoryOfTheOtherType() {
        String name = unique("val");
        long accountId = TestApp.account(name);
        long income = TestApp.category(accountId, "income", "Other type");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.post("/transactions", "type", "expense", "category_id", "" + income,
                "amount", "1000", "date", "2026-10-25"));
        assertTrue(page.contains("Choose a category that matches this transaction type."));
    }

    @Test
    void aTransactionCanBeEditedIncludingItsType() {
        String name = unique("edit");
        long accountId = TestApp.account(name);
        long expense = TestApp.category(accountId, "expense", "Edit out");
        long income = TestApp.category(accountId, "income", "Edit in");
        long transaction = TestApp.transaction(accountId, expense, "expense", 100, "2026-10-25", null);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        assertEquals(302, client.patch("/transactions/" + transaction, "type", "income", "category_id", "" + income,
                "amount", "250", "notes", "fixed", "date", "2026-10-01", "time", "08:15").statusCode());
        var row = Database.one("SELECT type, amount, categories_id, time FROM transactions WHERE id = ?",
                rs -> List.of(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getString(4)), transaction).orElseThrow();
        assertEquals(List.of("income", 250L, income, "08:15:00"), row);
    }

    @Test
    void aFailedEditReopensTheEditDialogWithTheInput() {
        String name = unique("edit");
        long accountId = TestApp.account(name);
        long expense = TestApp.category(accountId, "expense", "Edit out");
        long transaction = TestApp.transaction(accountId, expense, "expense", 100, "2026-10-25", "before");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.patch("/transactions/" + transaction, "type", "expense",
                "category_id", "" + expense, "amount", "0", "notes", "typed by hand", "date", "2026-10-25",
                "back", "/expense?q=bef"));
        assertTrue(page.contains("The amount field must be at least 1."));
        assertTrue(page.contains("id=\"transaction-edit\""));
        assertTrue(page.contains("data-open"));
        assertTrue(page.contains("typed by hand"));
        assertFalse(page.contains("id=\"transaction-new\""), "the new-entry dialog would show the same errors");
        assertEquals(100, count("SELECT amount FROM transactions WHERE id = ?", transaction));
    }

    @Test
    void anAccountCannotDeleteAnotherAccountsTransaction() {
        long owner = TestApp.account(unique("owner"));
        long category = TestApp.category(owner, "expense", "Owned");
        long transaction = TestApp.transaction(owner, category, "expense", 100, "2026-10-25", null);
        TestApp.Client client = TestApp.signedIn(unique("thief"));

        // laravel gives 403 here, we give 404
        assertEquals(404, client.delete("/transactions/" + transaction).statusCode());
        assertEquals(1, count("SELECT count(*) FROM transactions WHERE id = ?", transaction));
    }

    @Test
    void guestsCannotWriteDataOrExport() {
        TestApp.Client guest = new TestApp.Client();
        assertEquals("/login", location(guest.post("/transactions")));
        assertEquals("/login", location(guest.post("/categories")));
        assertEquals("/login", location(guest.get("/expense/export")));
        assertEquals("/login", location(guest.get("/recap/pdf")));
    }

    @Test
    void theTransactionListFiltersByDateRangeCategoryAndKeyword() {
        String name = unique("filter");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        long bills = TestApp.category(accountId, "expense", "Power");
        TestApp.transaction(accountId, food, "expense", 100, "2026-09-30", "old lunch");
        TestApp.transaction(accountId, food, "expense", 100, "2026-10-05", "lunch with team");
        TestApp.transaction(accountId, bills, "expense", 100, "2026-10-06", "electricity");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String range = client.get("/expense?from=2026-10-01&to=2026-10-31").body();
        assertTrue(range.contains("2 entries matching these filters · total Rp\u00a0200"));
        String category = client.get("/expense?category=" + bills).body();
        assertEquals(1, tableRows(category));
        assertTrue(category.contains("electricity"));
        String keyword = client.get("/expense?q=lunch").body();
        assertEquals(2, tableRows(keyword));
        assertTrue(keyword.contains("name=\"q\" type=\"text\" placeholder=\"Search notes...\" value=\"lunch\""));

        HttpResponse<String> bad = client.get("/expense?from=2026-10-31&to=2026-10-01");
        assertEquals(422, bad.statusCode());
        assertTrue(bad.body().contains("The to field must be a date after or equal to from."));
        assertEquals("/expense?from=2026-10-31&to=2026-10-01", location(client.get("/expense/export?from=2026-10-31&to=2026-10-01")));
    }

    @Test
    void theTransactionListIsPaginatedNewestFirst() {
        String name = unique("page");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "income", "Paged");
        for (int day = 1; day <= 20; day++) {
            TestApp.transaction(accountId, category, "income", 100, "2026-10-%02d".formatted(day), null);
        }
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String first = client.get("/income").body();
        assertEquals(15, tableRows(first));
        assertTrue(first.contains("Showing 1–15 of 20"));
        assertTrue(first.indexOf("2026-10-20") < first.indexOf("2026-10-19"));
        String second = client.get("/income?page=2").body();
        assertEquals(5, tableRows(second));
        assertTrue(second.contains("2026-10-01"));
    }

    @Test
    void csvExportContainsOnlyTheFilteredRowsOfTheAccount() {
        String name = unique("csv");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "Food");
        TestApp.transaction(accountId, category, "expense", 15000, "2026-10-05", "lunch");
        TestApp.transaction(accountId, category, "expense", 999, "2026-09-05", "outside range");
        long other = TestApp.account(unique("other"));
        TestApp.transaction(other, TestApp.category(other, "expense", "Food"), "expense", 5, "2026-10-05", "someone else");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        HttpResponse<String> res = client.get("/expense/export?from=2026-10-01");
        assertEquals(200, res.statusCode());
        assertTrue(res.headers().firstValue("Content-Type").orElse("").startsWith("text/csv"));
        assertEquals("attachment; filename=expense-transactions-" + App.today() + ".csv",
                res.headers().firstValue("Content-Disposition").orElse(null));
        List<String> lines = res.body().lines().filter(l -> !l.isEmpty()).toList();
        assertEquals(List.of("date,time,type,category,amount,notes", "2026-10-05,12:00:00,expense,Food,15000,lunch"), lines);
    }

    @Test
    void csvExportsNeutralizeSpreadsheetFormulasInUserText() {
        String name = unique("csv");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "@Food");
        TestApp.transaction(accountId, category, "expense", 500, "2026-10-05", "=HYPERLINK(\"http://evil\")");
        String expected = "2026-10-05,12:00:00,expense,'@Food,500,\"'=HYPERLINK(\"\"http://evil\"\")\"";

        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);
        assertTrue(client.get("/expense/export").body().contains(expected), "list export");
        assertTrue(client.get("/recap/csv?month=2026-10").body().contains(expected), "recap export");
    }

    @Test
    void aSoftDeletedCategoryIsHiddenAndItsNameCanBeReused() {
        String name = unique("soft");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "Snacks");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        assertEquals(302, client.delete("/categories/" + category).statusCode());
        assertEquals(1, count("SELECT count(*) FROM categories WHERE id = ? AND deleted_at IS NOT NULL", category));
        assertFalse(client.get("/expense").body().contains(">Snacks<"));

        String page = client.follow(client.post("/transactions", "type", "expense", "category_id", "" + category,
                "amount", "10", "date", "2026-10-25"));
        assertTrue(page.contains("Choose a category that matches this transaction type."));

        String again = client.follow(client.post("/categories", "name", "Snacks", "type", "expense"));
        assertFalse(again.contains("role=\"alert\""), "no validation errors");
        assertEquals(1, count("SELECT count(*) FROM categories WHERE accounts_id = ? AND name = 'Snacks' AND deleted_at IS NULL", accountId));
    }

    @Test
    void aCategoryInUseCannotSwitchTypeAndOrphanItsTransactions() {
        String name = unique("lock");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "Food");
        TestApp.transaction(accountId, category, "expense", 10, "2026-10-25", null);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.patch("/categories/" + category, "name", "Food", "type", "income"));
        assertTrue(page.contains("This category cannot change type while it has transactions."));
        assertEquals(1, count("SELECT count(*) FROM categories WHERE id = ? AND type = 'expense'", category));

        String renamed = client.follow(client.patch("/categories/" + category, "name", "Groceries", "type", "expense"));
        assertFalse(renamed.contains("role=\"alert\""), "no validation errors");
    }

    @Test
    void theDatabaseRejectsInvalidRowsEvenWithoutTheApp() {
        long accountId = TestApp.account(unique("db"));
        long category = TestApp.category(accountId, "expense", "Strict");
        long other = TestApp.account(unique("db"));
        String insert = "INSERT INTO transactions (type, amount, date, accounts_id, categories_id) VALUES (?, ?, '2026-10-25', ?, ?)";

        assertThrows(Database.DbException.class, () -> Database.update(insert, "expense", 0, accountId, category));
        assertThrows(Database.DbException.class, () -> Database.update(insert, "expense", 10, other, category));
        assertThrows(Database.DbException.class, () -> Database.update(insert, "income", 10, accountId, category));
        TestApp.transaction(accountId, category, "expense", 10, "2026-10-25", null);
        assertThrows(Database.DbException.class,
                () -> Database.update("UPDATE categories SET type = 'income' WHERE id = ?", category));
    }

    @Test
    void theRecapShowsAnEmptyStateWithNoData() {
        String page = TestApp.signedIn(unique("recap")).get("/recap?month=2020-01").body();
        assertTrue(page.contains("No data for this month"));
        assertTrue(page.contains(">+Rp\u00a00<"), "income");
        assertTrue(page.contains(">-Rp\u00a00<"), "expense");
        assertTrue(page.contains(">Rp\u00a00<"), "net");
    }

    @Test
    void theCategoryListShowsHowManyTransactionsEachCategoryHas() {
        String name = unique("count");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        TestApp.category(accountId, "expense", "Unused");
        for (int i = 0; i < 3; i++) {
            TestApp.transaction(accountId, food, "expense", 100, "2026-10-05", null);
        }
        long other = TestApp.account(unique("count"));
        TestApp.transaction(other, TestApp.category(other, "expense", "Food"), "expense", 100, "2026-10-05", null);

        String page = new TestApp.Client().login(name, TestApp.PASSWORD).get("/categories").body();
        assertTrue(page.matches("(?s).*>Food</span>\\s*<span class=\"text-xs text-muted\">3 entries</span>.*"), "Food count");
        assertTrue(page.matches("(?s).*>Unused</span>\\s*<span class=\"text-xs text-muted\">No entries</span>.*"), "Unused count");
    }

    @Test
    void userTextIsEscapedOnEveryPageThatShowsIt() {
        String evil = "<b id=x>\"'&";
        String name = unique("xss");
        long accountId = TestApp.account(name);
        Database.update("UPDATE accounts SET full_name = ? WHERE id = ?", evil, accountId);
        long category = TestApp.category(accountId, "expense", evil);
        long tx = TestApp.transaction(accountId, category, "expense", 100, App.today().toString(), evil);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        for (String path : List.of("/", "/expense", "/expense?edit=" + tx, "/categories", "/categories?edit=" + category,
                "/recap", "/recap/report", "/settings")) {
            String page = client.get(path).body();
            assertFalse(page.contains("<b id=x>"), path + " shows raw markup");
            assertTrue(page.contains("&lt;b id=x&gt;"), path + " shows the escaped text");
        }
    }

    @Test
    void deletingAnAccountDeletesItsCategoriesAndTransactionsOnly() {
        String name = unique("cascade");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        TestApp.transaction(accountId, food, "expense", 1000, "2026-10-25", null);
        long otherId = TestApp.account(unique("kept"));
        long otherFood = TestApp.category(otherId, "expense", "Food");
        TestApp.transaction(otherId, otherFood, "expense", 2000, "2026-10-25", null);

        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);
        assertEquals("/login", location(client.delete("/settings", "delete_password", TestApp.PASSWORD)));

        assertEquals(0, count("SELECT COUNT(*) FROM categories WHERE accounts_id = ?", accountId));
        assertEquals(0, count("SELECT COUNT(*) FROM transactions WHERE accounts_id = ?", accountId));
        assertTrue(count("SELECT COUNT(*) FROM categories WHERE accounts_id = ?", otherId) > 1);
        assertEquals(1, count("SELECT COUNT(*) FROM transactions WHERE accounts_id = ?", otherId));
    }
}
