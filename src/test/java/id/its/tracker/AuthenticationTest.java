package id.its.tracker;

import id.its.tracker.dao.AccountDao;
import id.its.tracker.database.Database;
import id.its.tracker.model.Account;
import id.its.tracker.service.Passwords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpResponse;
import java.util.List;

import static id.its.tracker.TestApp.PASSWORD;
import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationTest {

    private static final List<String> PROTECTED = List.of("/", "/income", "/expense", "/categories", "/recap", "/settings");

    private static boolean signedIn(TestApp.Client client) {
        return client.get("/").statusCode() == 200;
    }

    @Test
    void guestsAreRedirectedToLoginFromProtectedPages() {
        TestApp.Client guest = new TestApp.Client();
        for (String path : PROTECTED) {
            assertEquals("/login", location(guest.get(path)), path);
        }
    }

    @Test
    void guestsCanReachTheLoginAndRegisterPages() {
        TestApp.Client guest = new TestApp.Client();
        assertEquals(200, guest.get("/login").statusCode());
        assertEquals(200, guest.get("/register").statusCode());
    }

    @Test
    void authenticatedUsersAreRedirectedAwayFromGuestPages() {
        TestApp.Client client = TestApp.signedIn(unique("guestonly"));
        assertEquals("/", location(client.get("/login")));
        assertEquals("/", location(client.get("/register")));
    }

    @Test
    void guestsReturnToTheIntendedPageAfterLogin() {
        String name = unique("intended");
        TestApp.account(name);
        TestApp.Client client = new TestApp.Client();
        client.get("/?type=income");
        HttpResponse<String> res = client.post("/login", "login", name, "password", PASSWORD);
        assertEquals("/?type=income", location(res));
    }

    @Test
    void registrationCreatesAnAccountWithoutLoggingInAndRedirectsToLogin() {
        String name = unique("tsar");
        TestApp.Client client = new TestApp.Client();
        HttpResponse<String> res = client.post("/register", "username", name, "full_name", "Tsar User",
                "email", name + "@example.com", "password", PASSWORD, "password_confirmation", PASSWORD);
        assertEquals("/login", location(res));
        assertFalse(signedIn(client));

        Account account = AccountDao.findByUsername(name).orElseThrow();
        assertEquals("Tsar User", account.fullName());
        assertEquals(name + "@example.com", account.email());
        assertTrue(account.passwordHash().startsWith("$2y$12$"));
        assertTrue(Passwords.verify(PASSWORD, account.passwordHash()));
        assertEquals(13, Database.scalar("SELECT count(*) FROM categories WHERE accounts_id = ?", account.id()));
    }

    @Test
    void registrationRejectsMismatchedPasswordConfirmation() {
        String name = unique("mismatch");
        TestApp.Client client = new TestApp.Client();
        HttpResponse<String> res = client.post("/register", "username", name, "full_name", "Tsar User",
                "email", name + "@example.com", "password", PASSWORD, "password_confirmation", "different");
        assertEquals(422, res.statusCode());
        assertTrue(res.body().contains("The password field confirmation does not match."));
        assertTrue(res.body().contains("value=\"" + name + "\""), "old input is kept");
        assertFalse(res.body().contains(PASSWORD), "passwords are never echoed back");
        assertTrue(AccountDao.findByUsername(name).isEmpty());
    }

    @Test
    void registrationEnforcesThePasswordPolicy() {
        String name = unique("weak");
        TestApp.Client client = new TestApp.Client();
        HttpResponse<String> res = client.post("/register", "username", name, "full_name", "Weak",
                "email", name + "@example.com", "password", "password123", "password_confirmation", "password123");
        assertEquals(422, res.statusCode());
        assertTrue(res.body().contains("The password field must be at least 12 characters."));
        assertTrue(AccountDao.findByUsername(name).isEmpty());
    }

    @Test
    void loginAcceptsEmailOrUsernameAndErrorsOnBadCredentials() {
        String name = unique("tsar");
        long id = TestApp.account(name);

        TestApp.Client byEmail = new TestApp.Client();
        assertEquals("/", location(byEmail.post("/login", "login", name + "@example.com", "password", PASSWORD)));
        assertTrue(signedIn(byEmail));
        assertNotNull(AccountDao.find(id).orElseThrow().lastLogin());

        TestApp.Client byUsername = new TestApp.Client();
        assertEquals("/", location(byUsername.post("/login", "login", name, "password", PASSWORD)));
        assertTrue(signedIn(byUsername));

        TestApp.Client wrong = new TestApp.Client();
        HttpResponse<String> res = wrong.post("/login", "login", name + "@example.com", "password", "wrong");
        assertEquals(422, res.statusCode());
        assertTrue(res.body().contains("These credentials do not match our records."));
        assertFalse(signedIn(wrong));
    }

    @Test
    void loginCanRememberAnAccountAndTheCookieSignsInANewSession() {
        String name = unique("remember");
        long id = TestApp.account(name);
        TestApp.Client client = new TestApp.Client();
        assertEquals("/", location(client.post("/login", "login", name, "password", PASSWORD, "remember", "1")));

        String cookie = client.cookieValue("remember");
        assertNotNull(cookie);
        String stored = AccountDao.find(id).orElseThrow().rememberToken();
        assertNotNull(stored);
        assertFalse(cookie.contains(stored), "the database keeps only a hash of the token");

        TestApp.Client laterVisit = new TestApp.Client().cookie("remember", cookie);
        assertTrue(signedIn(laterVisit));

        TestApp.Client forged = new TestApp.Client().cookie("remember", id + "|forged");
        assertFalse(signedIn(forged));
    }

    @Test
    void rememberCookieIsSecureBehindAnHttpsProxy() {
        String name = unique("proxy");
        TestApp.account(name);
        TestApp.Client client = new TestApp.Client();
        client.get("/login");
        client.header("X-Forwarded-Proto", "https");
        HttpResponse<String> res = client.post("/login", "login", name, "password", PASSWORD, "remember", "1");
        String setCookie = res.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("remember=")).findFirst().orElseThrow();
        assertTrue(setCookie.contains("Secure"), setCookie);
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
    }

    @Test
    void logoutDestroysTheSessionAndForgetsTheRememberToken() {
        String name = unique("logout");
        long id = TestApp.account(name);
        TestApp.Client client = new TestApp.Client();
        client.post("/login", "login", name, "password", PASSWORD, "remember", "1");
        String cookie = client.cookieValue("remember");
        client.get("/");

        assertEquals("/login", location(client.post("/logout")));
        assertFalse(signedIn(client));
        assertNull(AccountDao.find(id).orElseThrow().rememberToken());
        assertFalse(signedIn(new TestApp.Client().cookie("remember", cookie)));
    }

    @Test
    void logoutIsPostOnly() {
        TestApp.Client client = TestApp.signedIn(unique("getlogout"));
        assertEquals(405, client.get("/logout").statusCode());
        assertTrue(signedIn(client));
    }

    @Test
    void loginIsRateLimitedAfterFiveAttempts() {
        String name = unique("throttle");
        TestApp.account(name);
        TestApp.Client client = new TestApp.Client();
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertEquals(422, client.post("/login", "login", name, "password", "wrong").statusCode());
        }
        HttpResponse<String> res = client.post("/login", "login", name, "password", PASSWORD);
        assertEquals(429, res.statusCode());
        assertTrue(res.headers().firstValue("Retry-After").isPresent());
        assertFalse(signedIn(client));
    }

    @Test
    void theSeededDemoAccountCanLogInAndIsSeededOnlyOnce() {
        Database.bootstrap();
        Database.bootstrap();
        Account demo = AccountDao.findByUsername("demo").orElseThrow();
        assertEquals(1, Database.scalar("SELECT count(*) FROM accounts WHERE username = 'demo'"));
        assertEquals(13, Database.scalar("SELECT count(*) FROM categories WHERE accounts_id = ?", demo.id()));
        assertEquals(18, Database.scalar("SELECT count(*) FROM transactions WHERE accounts_id = ?", demo.id()));
        assertEquals(0, Database.scalar("SELECT count(*) FROM transactions WHERE accounts_id = ? AND date > ?",
                demo.id(), App.today().toString()));
        assertEquals("/", location(new TestApp.Client().post("/login", "login", "demo", "password", "demo1234")));

        Database.update("UPDATE accounts SET username = 'demo-renamed' WHERE id = ?", demo.id());
        try {
            Database.bootstrap();
            assertEquals(1, Database.scalar("SELECT count(*) FROM accounts WHERE email = 'demo@example.com'"));
        } finally {
            Database.update("UPDATE accounts SET username = 'demo' WHERE id = ?", demo.id());
        }
    }

    @Test
    void dotlessEmailCanRegisterAndLogIn() {
        String name = unique("local");
        TestApp.Client client = new TestApp.Client();
        assertEquals("/login", location(client.post("/register", "username", name, "full_name", "Local User",
                "email", name + "@localhost", "password", PASSWORD, "password_confirmation", PASSWORD)));
        assertEquals("/", location(client.post("/login", "login", name + "@localhost", "password", PASSWORD)));
        assertTrue(signedIn(client));
    }

    @Test
    void usernamesAndEmailsAreUniqueRegardlessOfCapitalisation() {
        String name = unique("tsar");
        TestApp.account(name);
        HttpResponse<String> res = new TestApp.Client().post("/register", "username", name.toUpperCase(),
                "full_name", "Copy Cat", "email", name.toUpperCase() + "@Example.COM",
                "password", PASSWORD, "password_confirmation", PASSWORD);
        assertEquals(422, res.statusCode());
        assertTrue(res.body().contains("The username has already been taken."));
        assertTrue(res.body().contains("The email has already been taken."));
        assertEquals(1, Database.scalar("SELECT count(*) FROM accounts WHERE lower(username) = ?", name));
    }

    @Test
    void registrationStoresLowercaseAndLoginIgnoresCapitalisation() {
        String name = unique("NewUser");
        TestApp.Client client = new TestApp.Client();
        assertEquals("/login", location(client.post("/register", "username", name, "full_name", "New User",
                "email", "New." + name + "@Example.com", "password", PASSWORD, "password_confirmation", PASSWORD)));
        String lower = name.toLowerCase();
        Account account = AccountDao.findByUsername(lower).orElseThrow();
        assertEquals(lower, account.username());
        assertEquals("new." + lower + "@example.com", account.email());

        assertEquals("/", location(new TestApp.Client().post("/login", "login", name.toUpperCase(), "password", PASSWORD)));
        assertEquals("/", location(new TestApp.Client().post("/login",
                "login", "New." + name + "@EXAMPLE.com", "password", PASSWORD)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email"})
    void theDatabaseRejectsValuesThatDifferOnlyInCapitalisation(String column) {
        String name = unique("dbcase");
        TestApp.account(name);
        String username = column.equals("username") ? name.toUpperCase() : unique("other");
        String email = column.equals("email") ? (name + "@example.com").toUpperCase() : username + "@example.com";
        Database.DbException e = assertThrows(Database.DbException.class,
                () -> AccountDao.create(username, "Copy", email, "hash"));
        assertTrue(e.getMessage().contains("UNIQUE"), e.getMessage());
    }

    @Test
    void sessionIdChangesOnLogin() {
        String name = unique("fixation");
        TestApp.account(name);
        TestApp.Client client = new TestApp.Client();
        client.get("/login");
        String before = client.cookieValue("JSESSIONID");
        client.post("/login", "login", name, "password", PASSWORD);
        assertNotEquals(before, client.cookieValue("JSESSIONID"));
    }
}
