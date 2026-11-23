package id.its.tracker.controller;

import id.its.tracker.dao.AccountDao;
import id.its.tracker.database.Database;
import id.its.tracker.model.Account;
import id.its.tracker.service.AccountService;
import id.its.tracker.service.Passwords;
import id.its.tracker.util.Auth;
import id.its.tracker.util.LoginThrottle;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/login", "/register", "/logout"})
public class AuthController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        switch (Web.path(req)) {
            case "/login" -> Web.render(req, res, "auth/login");
            case "/register" -> Web.render(req, res, "auth/register");
            // POST only so other sites can't log people out
            default -> res.sendError(405);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        switch (Web.path(req)) {
            case "/login" -> store(req, res);
            case "/register" -> create(req, res);
            default -> destroy(req, res);
        }
    }

    private static void create(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String username = AccountService.lower(Web.param(req, "username"));
        String fullName = Web.param(req, "full_name");
        String email = AccountService.lower(Web.param(req, "email"));
        String password = Web.secret(req, "password");

        Map<String, String> errors = new LinkedHashMap<>();
        AccountService.checkIdentity(username, fullName, email, 0, errors);
        AccountService.checkNewPassword(password, Web.secret(req, "password_confirmation"), "password", errors);
        if (!errors.isEmpty()) {
            Map<String, String> old = Web.old(req, "full_name");
            old.put("username", username);
            old.put("email", email);
            Web.invalid(req, res, "auth/register", errors, old);
            return;
        }

        // the insert trigger copies the default categories
        try {
            AccountDao.create(username, fullName, email, Passwords.hash(password));
        } catch (Database.DbException e) {
            if (!e.isUniqueViolation()) {
                throw e;
            }
            AccountService.checkIdentity(username, fullName, email, 0, errors);
            Map<String, String> old = Web.old(req, "full_name");
            old.put("username", username);
            old.put("email", email);
            Web.invalid(req, res, "auth/register", errors, old);
            return;
        }
        res.sendRedirect("/login");
    }

    private static void store(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long wait = LoginThrottle.hit(req.getRemoteAddr());
        if (wait > 0) {
            res.setHeader("Retry-After", String.valueOf(wait));
            res.sendError(429);
            return;
        }

        String login = Web.param(req, "login");
        String password = Web.secret(req, "password");
        Map<String, String> errors = new LinkedHashMap<>();
        if (login == null) {
            errors.put("login", "The login field is required.");
        }
        if (password == null) {
            errors.put("password", "The password field is required.");
        }
        Map<String, String> old = Web.old(req, "login");
        if (!errors.isEmpty()) {
            Web.invalid(req, res, "auth/login", errors, old);
            return;
        }

        String key = login.toLowerCase(Locale.ROOT);
        Optional<Account> account = AccountDao.findByEmail(key).or(() -> AccountDao.findByUsername(key));
        if (account.isEmpty() || !Passwords.verify(password, account.get().passwordHash())) {
            Web.invalid(req, res, "auth/login", Map.of("login", "These credentials do not match our records."), old);
            return;
        }

        Auth.login(req, res, account.get(), req.getParameter("remember") != null);
        HttpSession session = req.getSession();
        Object intended = session.getAttribute(Web.INTENDED);
        session.removeAttribute(Web.INTENDED);
        res.sendRedirect(intended instanceof String path ? path : "/");
    }

    private static void destroy(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Auth.logout(req, res);
        res.sendRedirect("/login");
    }
}
