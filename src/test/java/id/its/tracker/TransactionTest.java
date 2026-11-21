package id.its.tracker;

import id.its.tracker.database.Database;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private static long count(String sql, Object... params) {
        return Database.scalar(sql, params);
    }

    @Test
    void anAccountCanRecordAndViewIncomeAndExpenseEntries() {
        String name = unique("tx");
        long accountId = TestApp.account(name);
        long salary = TestApp.category(accountId, "income", "Payroll");
        long food = TestApp.category(accountId, "expense", "Meals");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        assertEquals("/income", location(client.post("/transactions", "type", "income", "category_id", "" + salary,
                "amount", "5000000", "date", "2026-10-25", "time", "09:30", "notes", "October salary")));
        assertEquals("/expense", location(client.post("/transactions", "type", "expense", "category_id", "" + food,
                "amount", "75000", "date", "2026-10-25", "time", "12:15", "notes", "Lunch")));

        assertEquals(1, count("SELECT count(*) FROM transactions WHERE accounts_id = ? AND type = 'income' "
                + "AND amount = 5000000 AND categories_id = ? AND time = '09:30:00'", accountId, salary));

        String income = client.get("/income").body();
        assertTrue(income.contains("1 entry recorded · total Rp\u00a05.000.000"));
        assertTrue(income.contains("October salary"));
        String expense = client.get("/expense").body();
        assertTrue(expense.contains("1 entry recorded · total Rp\u00a075.000"));
        assertTrue(expense.contains("Lunch"));
    }

    @Test
    void anAccountCannotUseAnotherAccountCategoryOrEditAnotherAccountTransaction() {
        long other = TestApp.account(unique("owner"));
        long category = TestApp.category(other, "expense", "Theirs");
        long transaction = TestApp.transaction(other, category, "expense", 500, "2026-10-25", "theirs");
        TestApp.Client client = TestApp.signedIn(unique("tx"));

        String page = client.follow(client.post("/transactions", "type", "expense", "category_id", "" + category,
                "amount", "100", "date", "2026-10-25"));
        assertTrue(page.contains("Choose a category that matches this transaction type."));

        // laravel gives 403 here, we give 404
        HttpResponse<String> patch = client.patch("/transactions/" + transaction, "type", "expense",
                "category_id", "" + category, "amount", "200", "date", "2026-10-25");
        assertEquals(404, patch.statusCode());
        assertEquals(404, client.get("/expense?edit=" + transaction).statusCode());
        assertEquals(500, Database.scalar("SELECT amount FROM transactions WHERE id = ?", transaction));
    }

    @Test
    void deletingAnIncomeRedirectsToTheIncomePage() {
        deletingRedirectsToItsList("income");
    }

    @Test
    void deletingAnExpenseRedirectsToTheExpensePage() {
        deletingRedirectsToItsList("expense");
    }

    private static void deletingRedirectsToItsList(String type) {
        String name = unique("del");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, type, "Del " + type);
        long transaction = TestApp.transaction(accountId, category, type, 100, "2026-10-25", null);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        client.get("/");
        assertEquals("/" + type, location(client.post("/transactions/" + transaction, "_method", "DELETE", "back", "/")));
        assertEquals(0, count("SELECT count(*) FROM transactions WHERE id = ?", transaction));
    }
}
