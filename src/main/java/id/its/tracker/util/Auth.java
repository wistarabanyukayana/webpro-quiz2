package id.its.tracker.util;

import id.its.tracker.dao.AccountDao;
import id.its.tracker.filter.RequestFilter;
import id.its.tracker.model.Account;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

// cookie is "accountId|token", the db only keeps the sha-256 of the token
public final class Auth {

    static final String REMEMBER_COOKIE = "remember";
    private static final int REMEMBER_SECONDS = 30 * 24 * 60 * 60;

    private Auth() {
    }

    // new session id, against session fixation
    public static void login(HttpServletRequest req, HttpServletResponse res, Account account, boolean remember) {
        HttpSession session = req.getSession();
        req.changeSessionId();
        session.setAttribute(Web.ACCOUNT_ID, account.id());
        session.setAttribute(Web.SESSION_VERSION, account.sessionVersion());
        session.setAttribute(Web.CSRF, RequestFilter.newToken());
        AccountDao.touchLastLogin(account.id());
        if (remember) {
            String token = RequestFilter.newToken();
            AccountDao.setRememberToken(account.id(), sha256(token));
            setCookie(req, res, account.id() + "|" + token, REMEMBER_SECONDS);
        }
    }

    public static void keepAfterPasswordChange(HttpServletRequest req, HttpServletResponse res, long accountId, int version) {
        req.getSession().setAttribute(Web.SESSION_VERSION, version);
        Cookie[] cookies = req.getCookies();
        if (cookies != null && Arrays.stream(cookies).anyMatch(c -> REMEMBER_COOKIE.equals(c.getName()))) {
            String token = RequestFilter.newToken();
            AccountDao.setRememberToken(accountId, sha256(token));
            setCookie(req, res, accountId + "|" + token, REMEMBER_SECONDS);
        }
    }

    public static void logout(HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object id = session.getAttribute(Web.ACCOUNT_ID);
            if (id != null) {
                AccountDao.setRememberToken((Long) id, null);
            }
            session.invalidate();
        }
        setCookie(req, res, "", 0);
    }

    public static Optional<Account> current(HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(Web.ACCOUNT_ID) instanceof Long id) {
            Optional<Account> account = AccountDao.find(id);
            if (account.isPresent() && session.getAttribute(Web.SESSION_VERSION) instanceof Integer v
                    && v == account.get().sessionVersion()) {
                return account;
            }
            session.invalidate();
            return Optional.empty();
        }
        return fromCookie(req, res);
    }

    private static Optional<Account> fromCookie(HttpServletRequest req, HttpServletResponse res) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (!REMEMBER_COOKIE.equals(cookie.getName())) {
                continue;
            }
            String[] parts = cookie.getValue().split("\\|", 2);
            if (parts.length == 2 && parts[0].matches("[1-9][0-9]{0,17}")) {
                Optional<Account> account = AccountDao.find(Long.parseLong(parts[0]));
                if (account.isPresent() && account.get().rememberToken() != null && MessageDigest.isEqual(
                        account.get().rememberToken().getBytes(StandardCharsets.UTF_8),
                        sha256(parts[1]).getBytes(StandardCharsets.UTF_8))) {
                    HttpSession session = req.getSession();
                    req.changeSessionId();
                    session.setAttribute(Web.ACCOUNT_ID, account.get().id());
                    session.setAttribute(Web.SESSION_VERSION, account.get().sessionVersion());
                    return account;
                }
            }
            setCookie(req, res, "", 0);
        }
        return Optional.empty();
    }

    private static void setCookie(HttpServletRequest req, HttpServletResponse res, String value, int maxAge) {
        Cookie cookie = new Cookie(REMEMBER_COOKIE, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(req.isSecure());
        cookie.setMaxAge(maxAge);
        res.addCookie(cookie);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
