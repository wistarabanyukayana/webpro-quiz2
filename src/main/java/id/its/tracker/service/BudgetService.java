package id.its.tracker.service;

import id.its.tracker.dao.BudgetDao;
import id.its.tracker.dao.CategoryDao;
import id.its.tracker.model.Budget;

import java.util.Map;

public final class BudgetService {

    private BudgetService() {
    }

    public record Input(long categoryId, String month, long amount) {
    }

    public static Input validate(long accountId, Map<String, String> in, Budget existing, Map<String, String> errors) {
        Long categoryId = null;
        if (in.get("category_id") == null) {
            errors.put("category_id", "The category id field is required.");
        } else if ((categoryId = Rules.integer(in.get("category_id"))) == null) {
            errors.put("category_id", "The category id field must be an integer.");
        }

        String month = in.get("month");
        if (month == null) {
            errors.put("month", "The month field is required.");
        } else if (!Rules.isMonth(month)) {
            errors.put("month", "The month field must match the format Y-m.");
        }

        Long amount = null;
        if (in.get("amount") == null) {
            errors.put("amount", "The amount field is required.");
        } else if ((amount = Rules.integer(in.get("amount"))) == null) {
            errors.put("amount", "The amount field must be an integer.");
        } else if (amount < 1) {
            errors.put("amount", "The amount field must be at least 1.");
        }

        if (!errors.isEmpty()) {
            return null;
        }
        if (CategoryDao.find(accountId, categoryId).filter(c -> c.type().equals("expense")).isEmpty()) {
            errors.put("category_id", "Choose an expense category.");
            return null;
        }
        if (BudgetDao.taken(accountId, categoryId, month, existing == null ? 0 : existing.id())) {
            errors.put("category_id", "This category already has a budget for that month.");
            return null;
        }
        return new Input(categoryId, month, amount);
    }
}
