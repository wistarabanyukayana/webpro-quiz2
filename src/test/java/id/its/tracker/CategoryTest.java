package id.its.tracker;

import id.its.tracker.dao.CategoryDao;
import id.its.tracker.database.Database;
import org.junit.jupiter.api.Test;

import static id.its.tracker.TestApp.location;
import static id.its.tracker.TestApp.unique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTest {

    private static String deletedAt(long id) {
        return Database.query("SELECT deleted_at FROM categories WHERE id = ?", rs -> rs.getString(1), id).getFirst();
    }

    @Test
    void anAccountCanCreateAndUpdateItsCategories() {
        String name = unique("cat");
        TestApp.Client client = TestApp.signedIn(name);
        long accountId = id.its.tracker.dao.AccountDao.findByUsername(name).orElseThrow().id();

        assertEquals("/categories", location(client.post("/categories", "name", "Rent", "type", "expense")));
        long id = CategoryDao.list(accountId, "expense").stream().filter(c -> c.name().equals("Rent")).findFirst().orElseThrow().id();

        assertEquals("/categories", location(client.patch("/categories/" + id, "name", "Monthly rent", "type", "expense")));
        assertEquals("Monthly rent", CategoryDao.find(accountId, id).orElseThrow().name());
        assertTrue(client.get("/categories").body().contains("Monthly rent"));
    }

    @Test
    void categoryNamesMustBeUniqueForAnAccountAndType() {
        String name = unique("cat");
        long accountId = TestApp.account(name);
        TestApp.category(accountId, "expense", "Utilities");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.post("/categories", "name", "Utilities", "type", "expense"));
        assertTrue(page.contains("The name has already been taken."));
        // same name is fine for the other type
        String other = client.follow(client.post("/categories", "name", "Utilities", "type", "income"));
        assertTrue(!other.contains("The name has already been taken."));
    }

    @Test
    void anAccountCannotUpdateOrDeleteAnotherAccountsCategory() {
        long otherCategory = TestApp.category(TestApp.account(unique("owner")), "expense", "Private");
        TestApp.Client client = TestApp.signedIn(unique("cat"));

        assertEquals(404, client.patch("/categories/" + otherCategory, "name", "Updated", "type", "expense").statusCode());
        assertEquals(404, client.delete("/categories/" + otherCategory).statusCode());
        assertEquals(404, client.get("/categories?edit=" + otherCategory).statusCode());
        assertNull(deletedAt(otherCategory));
    }

    @Test
    void aCategoryWithTransactionsCannotBeDeleted() {
        String name = unique("cat");
        long accountId = TestApp.account(name);
        long category = TestApp.category(accountId, "expense", "Snacks");
        TestApp.transaction(accountId, category, "expense", 1000, "2026-10-25", null);
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        String page = client.follow(client.delete("/categories/" + category));
        assertTrue(page.contains("This category cannot be deleted while it has transactions."));
        assertNull(deletedAt(category));
    }

    @Test
    void anAccountCanDeleteAnUnusedCategory() {
        String name = unique("cat");
        long category = TestApp.category(TestApp.account(name), "expense", "Unused");
        TestApp.Client client = new TestApp.Client().login(name, TestApp.PASSWORD);

        assertEquals("/categories", location(client.delete("/categories/" + category)));
        assertNotNull(deletedAt(category));
    }
}
