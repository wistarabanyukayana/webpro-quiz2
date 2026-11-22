package id.its.tracker;

import id.its.tracker.dao.AccountDao;
import id.its.tracker.database.Database;
import id.its.tracker.model.Account;
import id.its.tracker.service.Passwords;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    private static final String NEW_PASSWORD = "New-Passw0rd-Long!";

    private static Account stored(String username) {
        return AccountDao.findByUsername(username).orElseThrow();
    }

    private static String profile(TestApp.Client client, String username, String email, String balance) {
        return client.follow(client.patch("/settings", "username", username, "full_name", "Tsar User",
                "email", email, "initial_balance", balance));
    }

    @Test
    void secretsOfTheAccountAreNeverSentToThePage() {
        String name = unique("set");
        long id = TestApp.account(name);
        Database.update("UPDATE accounts SET remember_token = 'secret-remember-token' WHERE id = ?", id);
        String page = new TestApp.Client().login(name, TestApp.PASSWORD).get("/settings").body();
        assertTrue(page.contains("value=\"" + name + "\""));
        assertFalse(page.contains("secret-remember-token"));
        assertFalse(page.contains(stored(name).passwordHash()));
    }

    @Test
    void aGuestCannotUpdateTheProfile() {
        assertEquals("/login", location(new TestApp.Client().patch("/settings", "username", "tsar")));
    }

    @Test
    void anAccountCanUpdateItsProfile() {
        String name = unique("set");
        TestApp.Client client = TestApp.signedIn(name);
        String renamed = unique("tsar");
        HttpResponse<String> res = client.patch("/settings", "username", renamed.toUpperCase(), "full_name", "Tsar User",
                "email", renamed + "@Example.com", "initial_balance", "50000");
        assertEquals("/settings", location(res));
        assertTrue(client.follow(res).contains(">Saved<"));

        Account a = stored(renamed);
        assertEquals(renamed, a.username());
        assertEquals("Tsar User", a.fullName());
        assertEquals(renamed + "@example.com", a.email());
        assertEquals(50000, a.initialBalance());
    }

    @Test
    void aProfileUpdateRejectsAUsernameOrEmailTakenByAnotherAccountInAnyCase() {
        String taken = unique("taken");
        TestApp.account(taken);
        TestApp.Client client = TestApp.signedIn(unique("set"));
        String page = profile(client, taken.toUpperCase(), taken.toUpperCase() + "@example.com", "0");
        assertTrue(page.contains("The username has already been taken."));
        assertTrue(page.contains("The email has already been taken."));
        assertTrue(page.contains("value=\"" + taken + "\""));
    }

    @Test
    void aProfileUpdateAcceptsUnchangedUniqueValues() {
        String name = unique("set");
        TestApp.Client client = TestApp.signedIn(name);
        String page = profile(client, name, name + "@example.com", "0");
        assertTrue(page.contains(">Saved<"));
        assertFalse(page.contains("role=\"alert\""));
    }

    @Test
    void theInitialBalanceMustBeANonNegativeInteger() {
        String name = unique("set");
        TestApp.Client client = TestApp.signedIn(name);
        assertTrue(profile(client, name, name + "@example.com", "-5").contains("The initial balance field must be at least 0."));
        assertTrue(profile(client, name, name + "@example.com", "1.5").contains("The initial balance field must be an integer."));
        assertTrue(profile(client, name, name + "@example.com", "").contains("The initial balance field is required."));
        assertEquals(0, stored(name).initialBalance());
    }

    @Test
    void passwordCanBeChangedWithTheCurrentPassword() {
        String name = unique("set");
        TestApp.Client client = TestApp.signedIn(name);
        HttpResponse<String> res = client.put("/settings/password", "current_password", TestApp.PASSWORD,
                "password", NEW_PASSWORD, "password_confirmation", NEW_PASSWORD);
        assertTrue(client.follow(res).contains(">Updated<"));
        assertTrue(Passwords.verify(NEW_PASSWORD, stored(name).passwordHash()));
    }

    @Test
    void passwordChangeFailsWithAWrongCurrentPassword() {
        String name = unique("set");
        TestApp.Client client = TestApp.signedIn(name);
        String page = client.follow(client.put("/settings/password", "current_password", "wrong-password",
                "password", NEW_PASSWORD, "password_confirmation", NEW_PASSWORD));
        assertTrue(page.contains("The password is incorrect."));
        assertTrue(Passwords.verify(TestApp.PASSWORD, stored(name).passwordHash()));
        assertTrue(page.contains("value=\"" + name + "\""));
    }

    @Test
    void passwordChangeRequiresAConfirmation() {
        TestApp.Client client = TestApp.signedIn(unique("set"));
        String page = client.follow(client.put("/settings/password", "current_password", TestApp.PASSWORD,
                "password", NEW_PASSWORD, "password_confirmation", "different"));
        assertTrue(page.contains("The password field confirmation does not match."));
    }

    @Test
    void changingThePasswordSignsOutOtherSessionsButKeepsThisOne() {
        String name = unique("set");
        TestApp.account(name);
        TestApp.Client here = new TestApp.Client().login(name, TestApp.PASSWORD);
        TestApp.Client elsewhere = new TestApp.Client().login(name, TestApp.PASSWORD);

        here.put("/settings/password", "current_password", TestApp.PASSWORD,
                "password", NEW_PASSWORD, "password_confirmation", NEW_PASSWORD);

        assertEquals(200, here.get("/").statusCode());
        assertEquals("/login", location(elsewhere.get("/")));
    }

    @Test
    void aRememberedDeviceStaysRememberedAndOldCookiesStopWorking() {
        String name = unique("set");
        TestApp.account(name);
        TestApp.Client here = new TestApp.Client();
        here.post("/login", "login", name, "password", TestApp.PASSWORD, "remember", "on");
        here.get("/settings");
        String before = here.cookieValue("remember");

        here.put("/settings/password", "current_password", TestApp.PASSWORD,
                "password", NEW_PASSWORD, "password_confirmation", NEW_PASSWORD);
        String after = here.cookieValue("remember");
        assertNotEquals(before, after);

        assertEquals("/login", location(new TestApp.Client().cookie("remember", before).get("/")));
        assertEquals(200, new TestApp.Client().cookie("remember", after).get("/").statusCode());
    }
}
