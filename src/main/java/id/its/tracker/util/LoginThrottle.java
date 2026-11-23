package id.its.tracker.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// in memory, resets on restart
public final class LoginThrottle {

    static final int MAX_ATTEMPTS = 5;
    static final long WINDOW_MILLIS = 60_000;

    private record Window(long start, int attempts) {
    }

    private static final Map<String, Window> WINDOWS = new ConcurrentHashMap<>();

    private LoginThrottle() {
    }

    public static long hit(String ip) {
        long now = System.currentTimeMillis();
        if (WINDOWS.size() > 10_000) {
            WINDOWS.values().removeIf(w -> now - w.start() >= WINDOW_MILLIS);
        }
        Window window = WINDOWS.compute(ip, (key, w) -> w == null || now - w.start() >= WINDOW_MILLIS
                ? new Window(now, 1)
                : new Window(w.start(), w.attempts() + 1));
        if (window.attempts() <= MAX_ATTEMPTS) {
            return 0;
        }
        return Math.max(1, (window.start() + WINDOW_MILLIS - now + 999) / 1000);
    }
}
