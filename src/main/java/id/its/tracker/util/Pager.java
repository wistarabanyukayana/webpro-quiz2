package id.its.tracker.util;

import id.its.tracker.service.Rules;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

public record Pager(long page, long total, long from, long to, List<Link> links) {

    public static final int PER_PAGE = 15;

    public record Link(String label, String url, boolean active) {
    }

    public static long page(HttpServletRequest req) {
        Long page = Rules.integer(Web.param(req, "page"));
        return page == null || page < 1 ? 1 : page;
    }

    public static long offset(long page) {
        return (page - 1) * PER_PAGE;
    }

    public static Pager of(HttpServletRequest req, long page, long total, int rowsShown) {
        long last = Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        List<Link> links = new ArrayList<>();
        links.add(new Link("« Previous", page > 1 ? Web.url(req, "page", String.valueOf(page - 1)) : null, false));
        for (long n : window(page, last)) {
            links.add(n == 0
                    ? new Link("...", null, false)
                    : new Link(String.valueOf(n), Web.url(req, "page", String.valueOf(n)), n == page));
        }
        links.add(new Link("Next »", page < last ? Web.url(req, "page", String.valueOf(page + 1)) : null, false));
        long from = rowsShown == 0 ? 0 : offset(page) + 1;
        return new Pager(page, total, from, rowsShown == 0 ? 0 : from + rowsShown - 1, links);
    }

    static List<Long> window(long page, long last) {
        List<Long> pages = new ArrayList<>();
        if (last < 14) {
            range(pages, 1, last);
        } else if (page <= 7) {
            range(pages, 1, 10);
            pages.add(0L);
            range(pages, last - 1, last);
        } else if (page > last - 7) {
            range(pages, 1, 2);
            pages.add(0L);
            range(pages, last - 9, last);
        } else {
            range(pages, 1, 2);
            pages.add(0L);
            range(pages, page - 3, page + 3);
            pages.add(0L);
            range(pages, last - 1, last);
        }
        return pages;
    }

    private static void range(List<Long> pages, long first, long last) {
        for (long n = first; n <= last; n++) {
            pages.add(n);
        }
    }
}
