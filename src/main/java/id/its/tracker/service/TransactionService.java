package id.its.tracker.service;

import id.its.tracker.dao.CategoryDao;
import id.its.tracker.dao.TransactionDao;
import id.its.tracker.model.Category;

import java.util.Map;

public final class TransactionService {

    private TransactionService() {
    }

    public record Input(String type, long categoryId, long amount, String notes, String date, String time) {
    }

    public static Input validate(long accountId, Map<String, String> in, Map<String, String> errors) {
        String type = in.get("type");
        if (type == null) {
            errors.put("type", "The type field is required.");
        } else if (!type.equals("income") && !type.equals("expense")) {
            errors.put("type", "The selected type is invalid.");
        }

        Long categoryId = null;
        if (in.get("category_id") == null) {
            errors.put("category_id", "The category id field is required.");
        } else if ((categoryId = Rules.integer(in.get("category_id"))) == null) {
            errors.put("category_id", "The category id field must be an integer.");
        }

        Long amount = null;
        if (in.get("amount") == null) {
            errors.put("amount", "The amount field is required.");
        } else if ((amount = Rules.integer(in.get("amount"))) == null) {
            errors.put("amount", "The amount field must be an integer.");
        } else if (amount < 1) {
            errors.put("amount", "The amount field must be at least 1.");
        }

        String notes = in.get("notes");
        if (notes != null && Rules.length(notes) > 1000) {
            errors.put("notes", "The notes field must not be greater than 1000 characters.");
        }

        String date = in.get("date");
        if (date == null) {
            errors.put("date", "The date field is required.");
        } else if (!Rules.isDate(date)) {
            errors.put("date", "The date field must match the format Y-m-d.");
        }

        String time = in.get("time");
        if (time != null && !Rules.isTime(time)) {
            errors.put("time", "The time field must match the format H:i.");
        }

        if (!errors.isEmpty()) {
            return null;
        }
        boolean matches = CategoryDao.find(accountId, categoryId).map(Category::type).filter(type::equals).isPresent();
        if (!matches) {
            errors.put("category_id", "Choose a category that matches this transaction type.");
            return null;
        }
        return new Input(type, categoryId, amount, notes, date, time == null ? null : time + ":00");
    }

    public static void create(long accountId, Input in) {
        TransactionDao.create(accountId, in.type(), in.categoryId(), in.amount(), in.notes(), in.date(), in.time());
    }

    public static void update(long accountId, long id, Input in) {
        TransactionDao.update(accountId, id, in.type(), in.categoryId(), in.amount(), in.notes(), in.date(), in.time());
    }
}
