package id.its.tracker;

import id.its.tracker.database.Database;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@WebListener
public class App implements ServletContextListener {

    public static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

    // tests override this
    public static volatile Clock clock = Clock.system(ZONE);

    public static LocalDate today() {
        return LocalDate.now(clock);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String path = System.getenv().getOrDefault("DB_PATH", System.getProperty("tracker.db", "data/tracker.db"));
        Database.init(Path.of(path));
        Database.bootstrap();
        event.getServletContext().log("Spending Tracker database ready at " + Path.of(path).toAbsolutePath());
    }
}
