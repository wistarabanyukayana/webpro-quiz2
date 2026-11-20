package id.its.tracker.util;

import id.its.tracker.model.Account;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class Web {

    public static final String CSRF = "csrf";
    public static final String ACCOUNT_ID = "accountId";
    public static final String SESSION_VERSION = "sessionVersion";
    public static final String INTENDED = "intended";
    private static final String FLASH = "flash";

    private record Flash(Map<String, String> errors, Map<String, String> old, String dialog) {
    }

    private Web() {
    }

    public static String path(HttpServletRequest req) {
        String info = req.getPathInfo();
        return req.getServletPath() + (info == null ? "" : info);
    }

    public static String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null) {
            return null;
        }
        value = value.strip();
        return value.isEmpty() ? null : value;
    }

    public static String secret(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null || value.isEmpty() ? null : value;
    }

    public static Account account(HttpServletRequest req) {
        return (Account) req.getAttribute("account");
    }

    public static Map<String, String> old(HttpServletRequest req, String... names) {
        Map<String, String> old = new HashMap<>();
        for (String name : names) {
            old.put(name, param(req, name));
        }
        return old;
    }

    public static String takeFlash(HttpServletRequest req) {
        Flash flash = req.getSession(false) == null ? null : (Flash) req.getSession().getAttribute(FLASH);
        if (flash != null) {
            req.getSession().removeAttribute(FLASH);
            req.setAttribute("errors", flash.errors());
            req.setAttribute("old", flash.old());
            req.setAttribute("openDialog", flash.dialog());
        }
        return (String) req.getAttribute("openDialog");
    }

    public static void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.setAttribute("currentPath", path(req));
        req.setAttribute("backUrl", url(req, null, null));
        takeFlash(req);
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    public static void invalid(HttpServletRequest req, HttpServletResponse resp, String view,
                               Map<String, String> errors, Map<String, String> old)
            throws ServletException, IOException {
        resp.setStatus(422);
        req.setAttribute("errors", errors);
        req.setAttribute("old", old);
        render(req, resp, view);
    }

    public static long idFromPath(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || !info.matches("/[1-9][0-9]{0,17}")) {
            return -1;
        }
        return Long.parseLong(info.substring(1));
    }

    public static String url(HttpServletRequest req, String name, String value) {
        StringJoiner query = new StringJoiner("&");
        req.getParameterMap().forEach((key, values) -> {
            if (!key.equals(name) && !key.equals("new") && !key.equals("edit")) {
                for (String v : values) {
                    query.add(encode(key) + "=" + encode(v));
                }
            }
        });
        if (value != null) {
            query.add(encode(name) + "=" + encode(value));
        }
        return path(req) + (query.length() == 0 ? "" : "?" + query);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static void back(HttpServletRequest req, HttpServletResponse resp, String fallback,
                            Map<String, String> errors, Map<String, String> old, String dialog) throws IOException {
        if (errors != null && !errors.isEmpty()) {
            req.getSession().setAttribute(FLASH, new Flash(errors, old, dialog));
        }
        String back = param(req, "back");
        resp.sendRedirect(back != null && back.matches("/(?![/\\\\])\\S*") ? back : fallback);
    }
}
