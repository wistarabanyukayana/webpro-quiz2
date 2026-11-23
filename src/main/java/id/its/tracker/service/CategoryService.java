package id.its.tracker.service;

import id.its.tracker.dao.BudgetDao;
import id.its.tracker.dao.CategoryDao;
import id.its.tracker.model.Category;

import java.util.Map;

public final class CategoryService {

    private CategoryService() {
    }

    public static boolean validate(long accountId, String name, String type, Category existing, Map<String, String> errors) {
        if (name == null) {
            errors.put("name", "The name field is required.");
        } else if (Rules.length(name) > 100) {
            errors.put("name", "The name field must not be greater than 100 characters.");
        } else if (CategoryDao.nameTaken(accountId, name, type, existing == null ? 0 : existing.id())) {
            errors.put("name", "The name has already been taken.");
        }
        if (type == null) {
            errors.put("type", "The type field is required.");
        } else if (!type.equals("income") && !type.equals("expense")) {
            errors.put("type", "The selected type is invalid.");
        }
        if (errors.isEmpty() && existing != null && !type.equals(existing.type())) {
            if (existing.transactionCount() > 0) {
                errors.put("type", "This category cannot change type while it has transactions.");
            } else if (BudgetDao.usesCategory(existing.id())) {
                errors.put("type", "This category cannot change type while it has budgets.");
            }
        }
        return errors.isEmpty();
    }

    public static boolean delete(long accountId, Category category, Map<String, String> errors) {
        if (category.transactionCount() > 0) {
            errors.put("category", "This category cannot be deleted while it has transactions.");
            return false;
        }
        CategoryDao.softDelete(accountId, category.id());
        return true;
    }
}
