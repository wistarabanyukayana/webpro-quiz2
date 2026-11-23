package id.its.tracker.controller;

import id.its.tracker.App;
import id.its.tracker.dao.BudgetDao;
import id.its.tracker.dao.CategoryDao;
import id.its.tracker.database.Database;
import id.its.tracker.model.Budget;
import id.its.tracker.service.BudgetService;
import id.its.tracker.service.Rules;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/budgets", "/budgets/*"})
public class BudgetController extends HttpServlet {

    private static final String LIST = "/budgets";
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getPathInfo() != null) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        String param = Web.param(req, "month");
        YearMonth month = YearMonth.from(App.today());
        if (param != null && Rules.isMonth(param)) {
            month = YearMonth.parse(param);
        } else if (param != null) {
            req.setAttribute("monthError", "The month field must match the format Y-m.");
        }
        List<Budget> budgets = BudgetDao.list(accountId, month.toString());
        req.setAttribute("budgets", budgets);
        req.setAttribute("month", month.toString());
        req.setAttribute("monthLabel", month.format(LABEL));
        req.setAttribute("prevMonth", month.minusMonths(1).toString());
        req.setAttribute("nextMonth", month.plusMonths(1).toString());
        req.setAttribute("budgeted", budgets.stream().mapToLong(Budget::amount).sum());
        req.setAttribute("spent", budgets.stream().mapToLong(Budget::spent).sum());
        req.setAttribute("categories", CategoryDao.list(accountId, "expense"));

        String dialog = Web.takeFlash(req);
        if (dialog != null && dialog.startsWith("edit-")) {
            BudgetDao.find(accountId, Long.parseLong(dialog.substring(5))).ifPresent(b -> req.setAttribute("editing", b));
        } else if (Web.param(req, "edit") != null) {
            String edit = Web.param(req, "edit");
            Optional<Budget> editing = edit.matches("[1-9][0-9]{0,17}") ? BudgetDao.find(accountId, Long.parseLong(edit)) : Optional.empty();
            if (editing.isEmpty()) {
                res.sendError(404);
                return;
            }
            req.setAttribute("editing", editing.get());
            req.setAttribute("openDialog", "edit-" + editing.get().id());
        } else if (Web.param(req, "new") != null) {
            req.setAttribute("openDialog", "new");
        }
        Web.render(req, res, "budgets");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (req.getPathInfo() != null) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        Map<String, String> old = Web.old(req, "category_id", "month", "amount");
        Map<String, String> errors = new HashMap<>();
        BudgetService.Input in = BudgetService.validate(accountId, old, null, errors);
        if (in == null) {
            Web.back(req, res, LIST, errors, old, "new");
            return;
        }
        try {
            BudgetDao.create(accountId, in.categoryId(), in.month(), in.amount());
        } catch (Database.DbException e) {
            if (!e.isUniqueViolation()) {
                throw e;
            }
            errors.put("category_id", "This category already has a budget for that month.");
            Web.back(req, res, LIST, errors, old, "new");
            return;
        }
        res.sendRedirect(LIST + "?month=" + in.month());
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<Budget> budget = owned(req);
        if (budget.isEmpty()) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        Map<String, String> old = Web.old(req, "category_id", "month", "amount");
        Map<String, String> errors = new HashMap<>();
        BudgetService.Input in = BudgetService.validate(accountId, old, budget.get(), errors);
        if (in == null) {
            Web.back(req, res, LIST, errors, old, "edit-" + budget.get().id());
            return;
        }
        try {
            BudgetDao.update(accountId, budget.get().id(), in.categoryId(), in.month(), in.amount());
        } catch (Database.DbException e) {
            if (!e.isUniqueViolation()) {
                throw e;
            }
            errors.put("category_id", "This category already has a budget for that month.");
            Web.back(req, res, LIST, errors, old, "edit-" + budget.get().id());
            return;
        }
        res.sendRedirect(LIST + "?month=" + in.month());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<Budget> budget = owned(req);
        if (budget.isEmpty()) {
            res.sendError(404);
            return;
        }
        BudgetDao.delete(Web.account(req).id(), budget.get().id());
        Web.back(req, res, LIST, Map.of(), Map.of(), null);
    }

    private static Optional<Budget> owned(HttpServletRequest req) {
        long id = Web.idFromPath(req);
        return id < 0 ? Optional.empty() : BudgetDao.find(Web.account(req).id(), id);
    }
}
