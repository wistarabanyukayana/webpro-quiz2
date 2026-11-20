package id.its.tracker.filter;

import id.its.tracker.model.Account;
import id.its.tracker.util.Auth;
import id.its.tracker.util.Web;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.MappingMatch;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class AuthFilter extends HttpFilter {

    private static final Set<String> GUEST_ONLY = Set.of("/login", "/register");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String path = Web.path(req);
        if (path.equals("/up") || req.getHttpServletMapping().getMappingMatch() == MappingMatch.DEFAULT) {
            chain.doFilter(req, res);
            return;
        }

        Optional<Account> account = Auth.current(req, res);
        if (GUEST_ONLY.contains(path)) {
            if (account.isPresent()) {
                res.sendRedirect("/");
                return;
            }
            chain.doFilter(req, res);
            return;
        }
        if (account.isEmpty()) {
            if ("GET".equals(req.getMethod())) {
                String query = req.getQueryString();
                req.getSession().setAttribute(Web.INTENDED, path + (query == null ? "" : "?" + query));
            }
            res.sendRedirect("/login");
            return;
        }
        req.setAttribute("account", account.get());
        chain.doFilter(req, res);
    }
}
