package id.its.tracker.controller;

import id.its.tracker.App;
import id.its.tracker.dao.CategoryDao;
import id.its.tracker.dao.TransactionDao;
import id.its.tracker.model.Transaction;
import id.its.tracker.service.TransactionFilters;
import id.its.tracker.service.TransactionService;
import id.its.tracker.util.Csv;
import id.its.tracker.util.Pager;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/income", "/expense", "/income/export", "/expense/export", "/transactions", "/transactions/*"})
public class TransactionController extends HttpServlet {

    private static final String[] FORM = {"type", "category_id", "amount", "notes", "date", "time"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getServletPath();
        if (path.equals("/transactions")) {
            res.sendError(req.getPathInfo() == null ? 405 : 404);
            return;
        }
        String type = path.startsWith("/income") ? "income" : "expense";
        long accountId = Web.account(req).id();

        Map<String, String> errors = new HashMap<>();
        TransactionFilters filters = TransactionFilters.parse(Web.old(req, "from", "to", "category", "q"), false, errors)
                .withType(type);
        if (path.endsWith("/export")) {
            if (errors.isEmpty()) {
                export(res, type, TransactionDao.all(accountId, filters));
            } else {
                res.sendRedirect(Web.url(req, "page", null).replaceFirst("^/(income|expense)/export", "/$1"));
            }
            return;
        }

        String dialog = Web.takeFlash(req);
        Transaction editing = null;
        if (dialog != null && dialog.startsWith("edit-")) {
            editing = TransactionDao.find(accountId, Long.parseLong(dialog.substring(5))).orElse(null);
        } else if (Web.param(req, "edit") != null) {
            String id = Web.param(req, "edit");
            Optional<Transaction> found = id.matches("[1-9][0-9]{0,17}")
                    ? TransactionDao.find(accountId, Long.parseLong(id)).filter(t -> t.type().equals(type))
                    : Optional.empty();
            if (found.isEmpty()) {
                res.sendError(404);
                return;
            }
            editing = found.get();
            req.setAttribute("openDialog", "edit-" + editing.id());
        } else if (Web.param(req, "new") != null) {
            req.setAttribute("openDialog", "new");
        }

        long page = Pager.page(req);
        long total = TransactionDao.count(accountId, filters);
        List<Transaction> rows = TransactionDao.page(accountId, filters, Pager.PER_PAGE, Pager.offset(page));
        req.setAttribute("type", type);
        req.setAttribute("rows", rows);
        req.setAttribute("pager", Pager.of(req, page, total, rows.size()));
        req.setAttribute("sum", TransactionDao.sum(accountId, filters));
        req.setAttribute("filtered", filters.any(false) || !errors.isEmpty());
        req.setAttribute("categories", CategoryDao.list(accountId, type));
        req.setAttribute("editing", editing);
        req.setAttribute("today", App.today().toString());
        req.setAttribute("nowTime", App.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        req.setAttribute("exportUrl", Web.url(req, "page", null).replaceFirst("^/(income|expense)", "/$1/export"));
        if (!errors.isEmpty()) {
            res.setStatus(422);
            req.setAttribute("filterErrors", errors);
        }
        Web.render(req, res, type);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!"/transactions".equals(req.getServletPath()) || req.getPathInfo() != null) {
            res.sendError(405);
            return;
        }
        long accountId = Web.account(req).id();
        Map<String, String> errors = new HashMap<>();
        TransactionService.Input input = TransactionService.validate(accountId, Web.old(req, FORM), errors);
        if (input != null) {
            TransactionService.create(accountId, input);
        }
        Web.back(req, res, list(Web.param(req, "type")), errors, Web.old(req, FORM), "new");
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<Transaction> transaction = owned(req);
        if (transaction.isEmpty()) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        Map<String, String> errors = new HashMap<>();
        TransactionService.Input input = TransactionService.validate(accountId, Web.old(req, FORM), errors);
        if (input != null) {
            TransactionService.update(accountId, transaction.get().id(), input);
        }
        Web.back(req, res, list(transaction.get().type()), errors, Web.old(req, FORM), "edit-" + transaction.get().id());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<Transaction> transaction = owned(req);
        if (transaction.isEmpty()) {
            res.sendError(404);
            return;
        }
        TransactionDao.delete(Web.account(req).id(), transaction.get().id());
        res.sendRedirect(list(transaction.get().type()));
    }

    private static Optional<Transaction> owned(HttpServletRequest req) {
        if (!"/transactions".equals(req.getServletPath())) {
            return Optional.empty();
        }
        long id = Web.idFromPath(req);
        return id < 0 ? Optional.empty() : TransactionDao.find(Web.account(req).id(), id);
    }

    private static String list(String type) {
        return "income".equals(type) ? "/income" : "/expense";
    }

    private static void export(HttpServletResponse res, String type, List<Transaction> rows) throws IOException {
        res.setContentType("text/csv");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=" + type + "-transactions-" + App.today() + ".csv");
        PrintWriter out = res.getWriter();
        out.print(Csv.row("date", "time", "type", "category", "amount", "notes"));
        for (Transaction t : rows) {
            out.print(Csv.row(t.date(), t.time(), t.type(), t.category(), t.amount(), t.notes()));
        }
    }
}
