package id.its.tracker.controller;

import id.its.tracker.dao.CategoryDao;
import id.its.tracker.database.Database;
import id.its.tracker.model.Category;
import id.its.tracker.service.CategoryService;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/categories", "/categories/*"})
public class CategoryController extends HttpServlet {

    private static final String LIST = "/categories";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getPathInfo() != null) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        List<Category> categories = CategoryDao.list(accountId);
        req.setAttribute("income", categories.stream().filter(c -> c.type().equals("income")).toList());
        req.setAttribute("expense", categories.stream().filter(c -> c.type().equals("expense")).toList());
        req.setAttribute("noCategories", categories.isEmpty());

        String dialog = Web.takeFlash(req);
        if (dialog != null && dialog.startsWith("edit-")) {
            CategoryDao.find(accountId, Long.parseLong(dialog.substring(5))).ifPresent(c -> req.setAttribute("editing", c));
        } else if (Web.param(req, "edit") != null) {
            Long editId = id(Web.param(req, "edit"));
            Optional<Category> editing = editId == null ? Optional.empty() : CategoryDao.find(accountId, editId);
            if (editing.isEmpty()) {
                res.sendError(404);
                return;
            }
            req.setAttribute("editing", editing.get());
            req.setAttribute("openDialog", "edit-" + editId);
        } else if (Web.param(req, "new") != null) {
            req.setAttribute("openDialog", "new");
        }
        Web.render(req, res, "categories");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (req.getPathInfo() != null) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        String name = Web.param(req, "name");
        String type = Web.param(req, "type");
        Map<String, String> errors = new HashMap<>();
        if (CategoryService.validate(accountId, name, type, null, errors)) {
            try {
                CategoryDao.create(accountId, name, type);
            } catch (Database.DbException e) {
                if (!e.isUniqueViolation()) {
                    throw e;
                }
                errors.put("name", "The name has already been taken.");
            }
        }
        Web.back(req, res, LIST, errors, Web.old(req, "name", "type"), "new");
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<Category> category = owned(req);
        if (category.isEmpty()) {
            res.sendError(404);
            return;
        }
        long accountId = Web.account(req).id();
        String name = Web.param(req, "name");
        String type = Web.param(req, "type");
        Map<String, String> errors = new HashMap<>();
        if (CategoryService.validate(accountId, name, type, category.get(), errors)) {
            try {
                CategoryDao.update(accountId, category.get().id(), name, type);
            } catch (Database.DbException e) {
                if (!e.isUniqueViolation()) {
                    throw e;
                }
                errors.put("name", "The name has already been taken.");
            }
        }
        Web.back(req, res, LIST, errors, Web.old(req, "name", "type"), "edit-" + category.get().id());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<Category> category = owned(req);
        if (category.isEmpty()) {
            res.sendError(404);
            return;
        }
        Map<String, String> errors = new HashMap<>();
        CategoryService.delete(Web.account(req).id(), category.get(), errors);
        Web.back(req, res, LIST, errors, Map.of(), null);
    }

    private static Optional<Category> owned(HttpServletRequest req) {
        long id = Web.idFromPath(req);
        return id < 0 ? Optional.empty() : CategoryDao.find(Web.account(req).id(), id);
    }

    private static Long id(String value) {
        return value != null && value.matches("[1-9][0-9]{0,17}") ? Long.valueOf(value) : null;
    }
}
