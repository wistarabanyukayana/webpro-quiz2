package id.its.tracker;

import id.its.tracker.database.Database;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaffoldTest {

    @Test
    void everyConnectionEnforcesForeignKeys() throws SQLException {
        TestApp.base();
        try (Connection c = Database.connect(); Statement s = c.createStatement()) {
            assertEquals(1, s.executeQuery("PRAGMA foreign_keys").getInt(1));
            SQLException e = assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO categories (name, type, accounts_id) VALUES ('Orphan', 'expense', 999999999)"));
            assertTrue(e.getMessage().contains("FOREIGN KEY"), e.getMessage());
        }
    }

    @Test
    void guestsAreSentToLoginAndTheDashboardOpensAfterLogin() {
        TestApp.Client client = new TestApp.Client();
        HttpResponse<String> home = client.get("/");
        assertEquals(302, home.statusCode());
        assertEquals("/login", TestApp.location(home));

        HttpResponse<String> login = client.get("/login");
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("Log in"));
        assertTrue(login.headers().firstValue("Content-Security-Policy").orElse("").contains("script-src 'self'"));
        assertEquals("nosniff", login.headers().firstValue("X-Content-Type-Options").orElse(null));
        assertEquals("DENY", login.headers().firstValue("X-Frame-Options").orElse(null));
        assertEquals("same-origin", login.headers().firstValue("Referrer-Policy").orElse(null));
        String session = home.headers().allValues("Set-Cookie").stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElse("");
        assertTrue(session.contains("HttpOnly") && session.contains("SameSite=Lax"), session);

        client.login("demo", "demo1234");
        HttpResponse<String> dashboard = client.get("/");
        assertEquals(200, dashboard.statusCode());
        assertTrue(dashboard.body().contains("Current balance"));
        assertEquals("/", TestApp.location(client.get("/login")));
    }

    @Test
    void healthCheckUnknownPathsAndMissingTokens() {
        TestApp.Client client = new TestApp.Client();
        assertEquals(200, client.get("/up").statusCode());
        HttpResponse<String> missing = client.get("/no-such-page");
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("Page not found"));
        client.get("/login");
        assertEquals(419, client.postRaw("/login", "login", "demo", "password", "demo1234").statusCode());
        var directDelete = java.net.http.HttpRequest.newBuilder(java.net.URI.create(TestApp.base() + "/transactions/1")).DELETE().build();
        try {
            assertEquals(419, client.http().send(directDelete, HttpResponse.BodyHandlers.ofString()).statusCode());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
