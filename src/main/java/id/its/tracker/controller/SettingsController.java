package id.its.tracker.controller;

import id.its.tracker.dao.AccountDao;
import id.its.tracker.model.Account;
import id.its.tracker.service.AccountService;
import id.its.tracker.service.Passwords;
import id.its.tracker.service.Rules;
import id.its.tracker.util.Auth;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet({"/settings", "/settings/password"})
public class SettingsController extends HttpServlet {

    private static final String SAVED = "settingsSaved";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!Web.path(req).equals("/settings")) {
            res.sendError(404);
            return;
        }
        Web.takeFlash(req);
        if (req.getAttribute("old") == null) {
            Account a = Web.account(req);
            Map<String, String> stored = new HashMap<>();
            stored.put("username", a.username());
            stored.put("full_name", a.fullName());
            stored.put("email", a.email());
            stored.put("initial_balance", String.valueOf(a.initialBalance()));
            req.setAttribute("old", stored);
        }
        req.setAttribute("saved", req.getSession().getAttribute(SAVED));
        req.getSession().removeAttribute(SAVED);
        Web.render(req, res, "settings");
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!Web.path(req).equals("/settings")) {
            res.sendError(405);
            return;
        }
        Account account = Web.account(req);
        String username = AccountService.lower(Web.param(req, "username"));
        String fullName = Web.param(req, "full_name");
        String email = AccountService.lower(Web.param(req, "email"));
        String balanceText = Web.param(req, "initial_balance");

        Map<String, String> errors = new LinkedHashMap<>();
        AccountService.checkIdentity(username, fullName, email, account.id(), errors);
        Long balance = balanceText == null ? null : Rules.integer(balanceText);
        if (balanceText == null) {
            errors.put("initial_balance", "The initial balance field is required.");
        } else if (balance == null) {
            errors.put("initial_balance", "The initial balance field must be an integer.");
        } else if (balance < 0) {
            errors.put("initial_balance", "The initial balance field must be at least 0.");
        }
        if (!errors.isEmpty()) {
            Map<String, String> old = Web.old(req, "full_name", "initial_balance");
            old.put("username", username);
            old.put("email", email);
            Web.back(req, res, "/settings", errors, old, null);
            return;
        }

        AccountDao.updateProfile(account.id(), username, fullName, email, balance);
        req.getSession().setAttribute(SAVED, "profile");
        Web.back(req, res, "/settings", null, null, null);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!Web.path(req).equals("/settings/password")) {
            res.sendError(405);
            return;
        }
        Account account = Web.account(req);
        String current = Web.secret(req, "current_password");
        String password = Web.secret(req, "password");

        Map<String, String> errors = new LinkedHashMap<>();
        if (current == null) {
            errors.put("current_password", "The current password field is required.");
        } else if (!Passwords.verify(current, account.passwordHash())) {
            errors.put("current_password", "The password is incorrect.");
        }
        AccountService.checkNewPassword(password, Web.secret(req, "password_confirmation"), "password", errors);
        if (!errors.isEmpty()) {
            Web.back(req, res, "/settings", errors, null, null);
            return;
        }

        int version = AccountDao.changePassword(account.id(), Passwords.hash(password));
        Auth.keepAfterPasswordChange(req, res, account.id(), version);
        req.getSession().setAttribute(SAVED, "password");
        Web.back(req, res, "/settings", null, null, null);
    }
}
