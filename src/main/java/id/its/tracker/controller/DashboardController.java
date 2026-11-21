package id.its.tracker.controller;

import id.its.tracker.App;
import id.its.tracker.dao.CategoryDao;
import id.its.tracker.dao.TransactionDao;
import id.its.tracker.model.Account;
import id.its.tracker.model.Transaction;
import id.its.tracker.service.TransactionFilters;
import id.its.tracker.util.Pager;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet("")
public class DashboardController extends HttpServlet {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Account account = Web.account(req);
        Map<String, String> errors = new HashMap<>();
        TransactionFilters filters = TransactionFilters.parse(Web.old(req, "from", "to", "category", "q", "type"), true, errors);

        LocalDate today = App.today();
        String monthStart = today.withDayOfMonth(1).toString();
        String monthEnd = today.withDayOfMonth(today.lengthOfMonth()).toString();
        TransactionFilters month = new TransactionFilters(filters.type(),
                max(filters.from(), monthStart), min(filters.to(), monthEnd), filters.category(), filters.q());

        long income = sum(account.id(), filters, "income");
        long expense = sum(account.id(), filters, "expense");
        long page = Pager.page(req);
        long total = TransactionDao.count(account.id(), filters);
        List<Transaction> rows = TransactionDao.page(account.id(), filters, Pager.PER_PAGE, Pager.offset(page));

        req.setAttribute("balance", account.initialBalance() + income - expense);
        req.setAttribute("monthIncome", sum(account.id(), month, "income"));
        req.setAttribute("monthExpense", sum(account.id(), month, "expense"));
        req.setAttribute("monthLabel", today.format(MONTH));
        req.setAttribute("rows", rows);
        req.setAttribute("pager", Pager.of(req, page, total, rows.size()));
        req.setAttribute("categories", CategoryDao.list(account.id()));
        req.setAttribute("filtered", filters.any(true) || !errors.isEmpty());
        if (!errors.isEmpty()) {
            res.setStatus(422);
            req.setAttribute("filterErrors", errors);
        }
        Web.render(req, res, "home");
    }

    private static long sum(long accountId, TransactionFilters filters, String type) {
        if (filters.type() != null && !filters.type().equals(type)) {
            return 0;
        }
        return TransactionDao.sum(accountId, filters.withType(type));
    }

    private static String max(String a, String b) {
        return a == null || b.compareTo(a) > 0 ? b : a;
    }

    private static String min(String a, String b) {
        return a == null || b.compareTo(a) < 0 ? b : a;
    }
}
