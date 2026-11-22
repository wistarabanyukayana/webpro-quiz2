package id.its.tracker;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecapTest {

    private static boolean listed(String page, String category, String amount) {
        return page.matches("(?s).*<span class=\"min-w-0 truncate text-sm text-ink\">" + category
                + "</span>\\s*<span class=\"[^\"]*\">" + amount + "</span>.*");
    }

    @Test
    void theRecapSummarizesTheSelectedMonthByCategory() {
        String name = unique("recap");
        long accountId = TestApp.account(name);
        long salary = TestApp.category(accountId, "income", "Pay");
        long food = TestApp.category(accountId, "expense", "Food");
        TestApp.transaction(accountId, salary, "income", 5_000_000, "2026-10-05", null);
        TestApp.transaction(accountId, food, "expense", 350_000, "2026-10-08", null);
        TestApp.transaction(accountId, salary, "income", 900_000, "2026-09-30", null);

        HttpResponse<String> res = new TestApp.Client().login(name, TestApp.PASSWORD).get("/recap?month=2026-10");
        String page = res.body();
        assertEquals(200, res.statusCode());
        assertTrue(page.contains("value=\"2026-10\""), "month");
        assertTrue(page.contains("Income and expense totals for October 2026."));
        assertTrue(page.contains(">+Rp\u00a05.000.000</p>"), "income");
        assertTrue(page.contains(">-Rp\u00a0350.000</p>"), "expense");
        assertTrue(page.contains(">+Rp\u00a04.650.000</p>"), "net");
        assertTrue(listed(page, "Pay", "\\+Rp\u00a05.000.000"), "income by category");
        assertTrue(listed(page, "Food", "-Rp\u00a0350.000"), "expense by category");
    }

    @Test
    void theRecapOnlyIncludesTransactionsForTheAuthenticatedAccount() {
        long other = TestApp.account(unique("recap"));
        TestApp.transaction(other, TestApp.category(other, "income", "Other"), "income", 999_999, "2026-10-10", null);

        String page = TestApp.signedIn(unique("recap")).get("/recap?month=2026-10").body();
        assertTrue(page.contains(">+Rp\u00a00</p>"));
        assertTrue(page.contains(">-Rp\u00a00</p>"));
        assertTrue(page.contains("No data for this month"));
        assertFalse(page.contains("999.999"));
    }

    @Test
    void theRecapSupportsARangeOfMonths() {
        String name = unique("recap");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "income", "Range");
        TestApp.transaction(accountId, category, "income", 100_000, "2026-01-15", null);
        TestApp.transaction(accountId, category, "income", 250_000, "2026-03-15", null);

        String page = new TestApp.Client().login(name, TestApp.PASSWORD)
                .get("/recap?mode=range&from=2026-01&to=2026-03").body();
        assertTrue(page.contains("<option value=\"range\" selected>"));
        assertTrue(page.contains("totals for January 2026 to March 2026."));
        assertTrue(page.contains(">+Rp\u00a0350.000</p>"));
    }

    @Test
    void theRecapSupportsAFullYearReport() {
        String name = unique("recap");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "Yearly");
        TestApp.transaction(accountId, category, "expense", 400_000, "2026-02-15", null);
        TestApp.transaction(accountId, category, "expense", 600_000, "2026-12-15", null);

        String page = new TestApp.Client().login(name, TestApp.PASSWORD).get("/recap?mode=year&year=2026").body();
        assertTrue(page.contains("<option value=\"year\" selected>"));
        assertTrue(page.contains("totals for 2026."));
        assertTrue(page.contains(">-Rp\u00a01.000.000</p>"));
    }

    @Test
    void theRecapExportsTheSelectedPeriodAsCsvHtmlAndPdf() {
        String name = unique("recap");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "income", "Consulting");
        TestApp.transaction(accountId, category, "income", 1_250_000, "2026-04-10", "April project");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);
        String query = "?mode=range&from=2026-04&to=2026-04";

        HttpResponse<String> csv = client.get("/recap/csv" + query);
        assertEquals("attachment; filename=\"recap-april-2026-to-april-2026.csv\"",
                csv.headers().firstValue("Content-Disposition").orElseThrow());
        assertEquals("text/csv;charset=UTF-8", csv.headers().firstValue("Content-Type").orElseThrow().replace(" ", ""));
        assertEquals("date,time,type,category,amount,notes\n2026-04-10,12:00:00,income,Consulting,1250000,\"April project\"\n", csv.body());

        HttpResponse<String> html = client.get("/recap/report" + query);
        assertEquals(200, html.statusCode());
        assertTrue(html.body().contains("Financial recap"));
        assertTrue(html.body().contains("April project"));

        HttpResponse<String> pdf = client.get("/recap/pdf" + query);
        assertEquals(200, pdf.statusCode());
        assertEquals("attachment; filename=recap-april-2026-to-april-2026.pdf",
                pdf.headers().firstValue("Content-Disposition").orElseThrow());
        assertEquals("application/pdf", pdf.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(pdf.body().startsWith("%PDF-"));
    }

    @Test
    void anInvalidPeriodShowsTheErrorsAndExportsGoBackToThePage() {
        TestApp.Client client = TestApp.signedIn(unique("recap"));
        HttpResponse<String> res = client.get("/recap?mode=range&from=2026-05&to=2026-04");
        assertEquals(422, res.statusCode());
        assertTrue(res.body().contains("The to field must be a date after or equal to from."));

        assertEquals(422, client.get("/recap?mode=week").statusCode());
        assertEquals(422, client.get("/recap?month=2026-13").statusCode());
        assertEquals(422, client.get("/recap?mode=year&year=26").statusCode());
        assertEquals("/recap?month=2026-13", location(client.get("/recap/pdf?month=2026-13")));
    }
}
