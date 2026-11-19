package id.its.tracker.database;

import java.sql.Connection;
import java.sql.SQLException;

final class DatabaseSeeder {

    private DatabaseSeeder() {
    }

    static void run(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try {
            DefaultCategorySeeder.run(c);
            DemoAccountSeeder.run(c);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }
}
