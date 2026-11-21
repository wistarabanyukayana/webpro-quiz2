package id.its.tracker;

import id.its.tracker.database.Database;
import org.junit.jupiter.api.Test;

import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardTest {

    private static final String TODAY = App.today().toString();

    private static int rows(String page) {
        return page.split("first:border-t-0").length - 1;
    }

    @Test
    void theDashboardShowsAccountBalanceAndCurrentMonthTotals() {
        String name = unique("dash");
        long accountId = TestApp.account(name);
        Database.update("UPDATE accounts SET initial_balance = 1000000 WHERE id = ?", accountId);
        long in = TestApp.category(accountId, "income", "Dash in");
        long out = TestApp.category(accountId, "expense", "Dash out");
        TestApp.transaction(accountId, in, "income", 500000, TODAY, null);
        TestApp.transaction(accountId, out, "expense", 125000, TODAY, null);

        String page = new TestApp.Client().login(name, TestApp.PASSWORD).get("/").body();
        assertTrue(page.contains(">Rp\u00a01.375.000<"), "balance");
        assertTrue(page.contains(">+Rp\u00a0500.000<"), "month income");
        assertTrue(page.contains(">-Rp\u00a0125.000<"), "month expense");
        assertTrue(page.contains("Showing 1–2 of 2"));
        assertEquals(2, rows(page));
    }

    @Test
    void theDashboardExcludesTransactionsFromAnotherMonthInMonthTotals() {
        String name = unique("dash");
        long accountId = TestApp.account(name);
        long in = TestApp.category(accountId, "income", "Dash in");
        TestApp.transaction(accountId, in, "income", 900000, App.today().minusMonths(1).toString(), null);

        String page = new TestApp.Client().login(name, TestApp.PASSWORD).get("/").body();
        assertTrue(page.contains(">Rp\u00a0900.000<"), "balance");
        assertTrue(page.contains(">+Rp\u00a00<"), "month income");
        assertTrue(page.contains(">-Rp\u00a00<"), "month expense");
    }

    @Test
    void theDashboardFiltersTransactionsAndTotalsByTheSelectedFilters() {
        String name = unique("dash");
        long accountId = TestApp.account(name);
        long in = TestApp.category(accountId, "income", "Dash in");
        long out = TestApp.category(accountId, "expense", "Dash out");
        TestApp.transaction(accountId, in, "income", 800000, TODAY, "Salary payment");
        TestApp.transaction(accountId, out, "expense", 100000, TODAY, "Groceries");

        String page = new TestApp.Client().login(name, TestApp.PASSWORD).get("/?type=income&q=salary").body();
        assertTrue(page.contains(">Rp\u00a0800.000<"), "balance");
        assertTrue(page.contains(">+Rp\u00a0800.000<"), "month income");
        assertTrue(page.contains(">-Rp\u00a00<"), "month expense");
        assertEquals(1, rows(page));
        assertTrue(page.contains("Salary payment"));
        assertTrue(page.contains("<option value=\"income\" selected>"), "the type filter stays selected");
    }

    @Test
    void invalidDashboardFiltersAreRejected() {
        TestApp.Client client = TestApp.signedIn(unique("dash"));
        var res = client.get("/?from=2026-10-31&to=2026-10-01&type=transfer");
        assertEquals(422, res.statusCode());
        assertTrue(res.body().contains("The to field must be a date after or equal to from."));
        assertTrue(res.body().contains("The selected type is invalid."));
    }
}
