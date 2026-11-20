package id.its.tracker.filter;

import id.its.tracker.util.Web;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

public class RequestFilter extends HttpFilter {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> OVERRIDES = Set.of("PATCH", "PUT", "DELETE");
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            // charts use inline styles
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "same-origin");
        res.setHeader("Content-Security-Policy", CSP);

        String path = Web.path(req);
        if (path.startsWith("/assets/") || path.equals("/up")) {
            chain.doFilter(req, res);
            return;
        }

        if ("POST".equals(req.getMethod())) {
            HttpSession session = req.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute(Web.CSRF);
            String given = req.getParameter("_token");
            if (expected == null || given == null || !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
                res.sendError(419);
                return;
            }
            String override = req.getParameter("_method");
            if (override != null && OVERRIDES.contains(override.toUpperCase())) {
                String method = override.toUpperCase();
                req = new HttpServletRequestWrapper(req) {
                    @Override
                    public String getMethod() {
                        return method;
                    }
                };
            }
        } else {
            HttpSession session = req.getSession();
            if (session.getAttribute(Web.CSRF) == null) {
                session.setAttribute(Web.CSRF, newToken());
            }
        }
        chain.doFilter(req, res);
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
