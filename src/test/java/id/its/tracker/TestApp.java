package id.its.tracker;

import id.its.tracker.database.Database;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestApp {

    private static final Pattern TOKEN = Pattern.compile("name=\"_token\" value=\"([^\"]+)\"");
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static String base;

    private TestApp() {
    }

    public static synchronized String base() {
        if (base == null) {
            try {
                start();
            } catch (IOException | LifecycleException e) {
                throw new IllegalStateException(e);
            }
        }
        return base;
    }

    private static void start() throws IOException, LifecycleException {
        Path dir = Files.createTempDirectory("tracker-test");
        System.setProperty("tracker.db", dir.resolve("tracker.db").toString());
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(dir.resolve("tomcat").toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        Context ctx = tomcat.addWebapp("", Path.of("src/main/webapp").toAbsolutePath().toString());
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                Path.of("target/classes").toAbsolutePath().toString(), "/"));
        ctx.setResources(resources);
        tomcat.start();
        Database.init(Path.of(System.getProperty("tracker.db")));
        base = "http://localhost:" + tomcat.getConnector().getLocalPort();
    }

    public static String unique(String prefix) {
        return prefix + COUNTER.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);
    }

    public static final class Client {
        private final HttpClient http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        private String token;
        // different ip per client so the login throttle doesn't kick in
        private final String ip = "2001:db8::" + Integer.toHexString(COUNTER.incrementAndGet());
        private final Map<String, String> headers = new LinkedHashMap<>();

        public Client header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Client cookie(String name, String value) {
            java.net.HttpCookie cookie = new java.net.HttpCookie(name, value);
            cookie.setPath("/");
            cookie.setVersion(0);
            ((CookieManager) http.cookieHandler().orElseThrow()).getCookieStore().add(URI.create(base()), cookie);
            return this;
        }

        public String cookieValue(String name) {
            return ((CookieManager) http.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals(name)).map(java.net.HttpCookie::getValue).findFirst().orElse(null);
        }

        public HttpResponse<String> get(String path) {
            HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(base() + path)).GET());
            Matcher m = TOKEN.matcher(res.body());
            if (m.find()) {
                token = m.group(1);
            }
            return res;
        }

        public HttpResponse<String> post(String path, String... pairs) {
            if (token == null) {
                get("/login");
            }
            List<String> all = new ArrayList<>(List.of("_token", token));
            all.addAll(List.of(pairs));
            return postRaw(path, all.toArray(String[]::new));
        }

        public HttpResponse<String> postRaw(String path, String... pairs) {
            Map<String, String> form = new LinkedHashMap<>();
            for (int i = 0; i < pairs.length; i += 2) {
                form.put(pairs[i], pairs[i + 1]);
            }
            StringBuilder body = new StringBuilder();
            form.forEach((k, v) -> body.append(body.isEmpty() ? "" : "&")
                    .append(URLEncoder.encode(k, StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8)));
            return send(HttpRequest.newBuilder(URI.create(base() + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
        }

        public HttpClient http() {
            return http;
        }

        private HttpResponse<String> send(HttpRequest.Builder request) {
            request.header("X-Forwarded-For", ip);
            headers.forEach(request::header);
            try {
                return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        public HttpResponse<String> patch(String path, String... pairs) {
            List<String> all = new ArrayList<>(List.of("_method", "PATCH"));
            all.addAll(List.of(pairs));
            return post(path, all.toArray(String[]::new));
        }

        public HttpResponse<String> put(String path, String... pairs) {
            List<String> all = new ArrayList<>(List.of("_method", "PUT"));
            all.addAll(List.of(pairs));
            return post(path, all.toArray(String[]::new));
        }

        public HttpResponse<String> delete(String path, String... pairs) {
            List<String> all = new ArrayList<>(List.of("_method", "DELETE"));
            all.addAll(List.of(pairs));
            return post(path, all.toArray(String[]::new));
        }

        public String follow(HttpResponse<String> redirect) {
            if (redirect.statusCode() != 302) {
                throw new AssertionError("Expected a redirect, got " + redirect.statusCode());
            }
            return get(location(redirect)).body();
        }

        public Client login(String login, String password) {
            HttpResponse<String> res = post("/login", "login", login, "password", password);
            if (res.statusCode() != 302) {
                throw new AssertionError("Login failed with " + res.statusCode());
            }
            get("/");
            return this;
        }
    }

    public static final String PASSWORD = "Sp1ce-R0ute-Long!";

    public static long account(String username) {
        base();
        return id.its.tracker.dao.AccountDao.create(username, "Test " + username, username + "@example.com",
                id.its.tracker.service.Passwords.hash(PASSWORD));
    }

    public static Client signedIn(String username) {
        account(username);
        return new Client().login(username, PASSWORD);
    }

    public static long category(long accountId, String type, String name) {
        return id.its.tracker.dao.CategoryDao.create(accountId, name, type);
    }

    public static long transaction(long accountId, long categoryId, String type, long amount, String date, String notes) {
        return id.its.tracker.dao.TransactionDao.create(accountId, type, categoryId, amount, notes, date, "12:00:00");
    }

    public static String location(HttpResponse<?> res) {
        return res.headers().firstValue("Location").map(l -> l.replace(base(), "")).orElse(null);
    }
}
