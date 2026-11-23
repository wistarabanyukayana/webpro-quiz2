package id.its.tracker;

import id.its.tracker.database.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetTest {

    private static long count(String sql, Object... params) {
        return Database.scalar(sql, params);
    }

    private static long amount(long budget) {
        return count("SELECT amount FROM budgets WHERE id = ?", budget);
    }

    @Test
    void anAccountCanCreateEditAndDeleteABudget() {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        assertEquals("/budgets?month=2026-10",
                location(client.post("/budgets", "category_id", "" + food, "month", "2026-10", "amount", "500000")));
        long budget = count("SELECT id FROM budgets WHERE accounts_id = ? AND month = '2026-10'", accountId);
        assertEquals(500_000, amount(budget));

        assertEquals("/budgets?month=2026-11",
                location(client.patch("/budgets/" + budget, "category_id", "" + food, "month", "2026-11", "amount", "750000")));
        assertEquals(750_000, amount(budget));
        assertEquals(1, count("SELECT count(*) FROM budgets WHERE id = ? AND month = '2026-11'", budget));

        assertEquals("/budgets?month=2026-11", location(client.delete("/budgets/" + budget, "back", "/budgets?month=2026-11")));
        assertEquals(0, count("SELECT count(*) FROM budgets WHERE accounts_id = ?", accountId));
    }

    @Test
    void theListShowsWhatWasSpentInThatCategoryAndMonthOnly() {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        long rent = TestApp.category(accountId, "expense", "Rent");
        TestApp.budget(accountId, food, "2026-10", 100_000);
        TestApp.budget(accountId, rent, "2026-10", 1_000_000);
        TestApp.transaction(accountId, food, "expense", 30_000, "2026-10-01", null);
        TestApp.transaction(accountId, food, "expense", 50_000, "2026-10-31", null);
        TestApp.transaction(accountId, food, "expense", 99_000, "2026-11-01", null);
        TestApp.transaction(accountId, rent, "expense", 1_200_000, "2026-10-05", null);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.get("/budgets?month=2026-10").body();
        assertTrue(page.contains("Spending limits for October 2026."));
        assertTrue(page.contains("-Rp\u00a080.000</span> of Rp\u00a0100.000"), "Food spent counts October only");
        assertTrue(page.contains("Rp\u00a020.000 left"));
        assertTrue(page.contains("80%"));
        assertTrue(page.contains("Over by Rp\u00a0200.000"), "Rent is over its limit");
        assertTrue(page.contains("120%"));
        // 1.1M budgeted, 1.28M spent
        assertTrue(page.contains("Rp\u00a01.100.000"));
        assertTrue(page.contains("-Rp\u00a01.280.000"));
        assertTrue(page.contains("Over budget by"));

        assertTrue(client.get("/budgets?month=2026-12").body().contains("No budgets for December 2026"));
    }

    @Test
    void aBadMonthInTheQueryFallsBackToTheCurrentMonth() {
        TestApp.Client client = TestApp.signedIn(unique("bud"));
        String page = client.get("/budgets?month=2026-13").body();
        assertTrue(page.contains("The month field must match the format Y-m."));
        assertTrue(page.contains("value=\"" + App.today().toString().substring(0, 7) + "\""));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "missing category | category_id | ''      | The category id field is required.",
        "text category    | category_id | abc     | The category id field must be an integer.",
        "missing month    | month       | ''      | The month field is required.",
        "bad month        | month       | 2026-13 | The month field must match the format Y-m.",
        "day in month     | month       | 2026-10-01 | The month field must match the format Y-m.",
        "zero amount      | amount      | 0       | The amount field must be at least 1.",
        "decimal amount   | amount      | 10.5    | The amount field must be an integer.",
        "missing amount   | amount      | ''      | The amount field is required.",
    })
    void budgetInputIsValidated(String label, String field, String value, String message) {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        List<String> form = new ArrayList<>(List.of("category_id", "" + food, "month", "2026-10", "amount", "1000"));
        form.set(form.indexOf(field) + 1, value == null ? "" : value);
        String page = client.follow(client.post("/budgets", form.toArray(String[]::new)));

        assertTrue(page.contains(message), label);
        assertTrue(page.contains("data-open"), "the add dialog reopens");
        assertEquals(0, count("SELECT count(*) FROM budgets WHERE accounts_id = ?", accountId));
    }

    @Test
    void onlyTheAccountsOwnActiveExpenseCategoriesCanHaveABudget() {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long salary = TestApp.category(accountId, "income", "Wages");
        long gone = TestApp.category(accountId, "expense", "Gone");
        Database.update("UPDATE categories SET deleted_at = datetime('now') WHERE id = ?", gone);
        long foreign = TestApp.category(TestApp.account(unique("owner")), "expense", "Theirs");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        for (long category : List.of(salary, gone, foreign)) {
            String page = client.follow(client.post("/budgets", "category_id", "" + category, "month", "2026-10", "amount", "1000"));
            assertTrue(page.contains("Choose an expense category."), "category " + category);
        }
        assertEquals(0, count("SELECT count(*) FROM budgets WHERE categories_id IN (?, ?, ?)", salary, gone, foreign));
    }

    @Test
    void aCategoryHasOneBudgetPerMonth() {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        long existing = TestApp.budget(accountId, food, "2026-10", 1000);
        long november = TestApp.budget(accountId, food, "2026-11", 1000);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.post("/budgets", "category_id", "" + food, "month", "2026-10", "amount", "2000"));
        assertTrue(page.contains("This category already has a budget for that month."));
        page = client.follow(client.patch("/budgets/" + november, "category_id", "" + food, "month", "2026-10", "amount", "2000"));
        assertTrue(page.contains("This category already has a budget for that month."));
        assertEquals("/budgets?month=2026-10",
                location(client.patch("/budgets/" + existing, "category_id", "" + food, "month", "2026-10", "amount", "3000")));
        assertEquals(3000, amount(existing));
    }

    @Test
    void aFailedEditReopensTheEditDialogWithTheInput() {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long food = TestApp.category(accountId, "expense", "Food");
        long budget = TestApp.budget(accountId, food, "2026-10", 1000);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.patch("/budgets/" + budget, "category_id", "" + food, "month", "2026-10", "amount", "-7"));
        assertTrue(page.contains("The amount field must be at least 1."));
        assertTrue(page.contains("id=\"budget-edit\" aria-labelledby=\"budget-edit-title\" closedby=\"any\" data-open"));
        assertTrue(page.contains("value=\"-7\""));
        assertFalse(page.contains("id=\"budget-new\""), "the add dialog is left out");
        assertEquals(1000, amount(budget));
    }

    @Test
    void anAccountCannotSeeEditOrDeleteAnotherAccountsBudget() {
        long ownerId = TestApp.account(unique("owner"));
        long theirs = TestApp.category(ownerId, "expense", "Private");
        long budget = TestApp.budget(ownerId, theirs, "2026-10", 1000);
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long mine = TestApp.category(accountId, "expense", "Mine");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        assertEquals(404, client.get("/budgets?edit=" + budget).statusCode());
        assertEquals(404, client.patch("/budgets/" + budget, "category_id", "" + mine, "month", "2026-10", "amount", "5").statusCode());
        assertEquals(404, client.delete("/budgets/" + budget).statusCode());
        assertFalse(client.get("/budgets?month=2026-10").body().contains("Private"));
        assertEquals(1000, amount(budget));
    }

    @Test
    void deletingACategoryDeletesItsBudgetsAndABudgetLocksItsType() {
        String name = unique("bud");
        long accountId = TestApp.account(name);
        long snacks = TestApp.category(accountId, "expense", "Snacks");
        long rent = TestApp.category(accountId, "expense", "Rent");
        TestApp.budget(accountId, snacks, "2026-10", 1000);
        TestApp.budget(accountId, rent, "2026-10", 1000);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.patch("/categories/" + rent, "name", "Rent", "type", "income"));
        assertTrue(page.contains("This category cannot change type while it has budgets."));
        assertEquals(1, count("SELECT count(*) FROM categories WHERE id = ? AND type = 'expense'", rent));

        assertEquals("/categories", location(client.delete("/categories/" + snacks)));
        assertEquals(0, count("SELECT count(*) FROM budgets WHERE categories_id = ?", snacks));
        assertEquals(1, count("SELECT count(*) FROM budgets WHERE categories_id = ?", rent));
    }

    @Test
    void theDatabaseRejectsInvalidBudgetsEvenWithoutTheApp() {
        long accountId = TestApp.account(unique("bud"));
        long food = TestApp.category(accountId, "expense", "Food");
        long salary = TestApp.category(accountId, "income", "Wages");
        long foreign = TestApp.category(TestApp.account(unique("owner")), "expense", "Theirs");
        String insert = "INSERT INTO budgets (month, amount, accounts_id, categories_id) VALUES (?, ?, ?, ?)";

        assertThrows(Database.DbException.class, () -> Database.update(insert, "2026-10", 0, accountId, food));
        assertThrows(Database.DbException.class, () -> Database.update(insert, "2026-13", 10, accountId, food));
        assertThrows(Database.DbException.class, () -> Database.update(insert, "2026-10", 10, accountId, salary));
        assertThrows(Database.DbException.class, () -> Database.update(insert, "2026-10", 10, accountId, foreign));
        Database.update(insert, "2026-10", 10, accountId, food);
        assertThrows(Database.DbException.class, () -> Database.update(insert, "2026-10", 20, accountId, food));
        assertThrows(Database.DbException.class, () -> Database.update("UPDATE categories SET type = 'income' WHERE id = ?", food));
    }

    @Test
    void theDemoAccountHasBudgetsForThisMonth() {
        long demo = count("SELECT id FROM accounts WHERE username = 'demo'");
        String month = App.today().toString().substring(0, 7);
        assertEquals(4, count("SELECT count(*) FROM budgets WHERE accounts_id = ? AND month = ?", demo, month));
    }
}
