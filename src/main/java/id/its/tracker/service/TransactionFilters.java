package id.its.tracker.service;

import java.util.Map;

public record TransactionFilters(String type, String from, String to, Long category, String q) {

    public static final TransactionFilters NONE = new TransactionFilters(null, null, null, null, null);

    public TransactionFilters withType(String value) {
        return new TransactionFilters(value, from, to, category, q);
    }

    public boolean any(boolean typeIsAFilter) {
        return (typeIsAFilter && type != null) || from != null || to != null || category != null || q != null;
    }

    public static TransactionFilters parse(Map<String, String> input, boolean withType, Map<String, String> errors) {
        String from = input.get("from");
        String to = input.get("to");
        String categoryText = input.get("category");
        String q = input.get("q");
        String type = withType ? input.get("type") : null;
        int before = errors.size();

        if (from != null && !Rules.isDate(from)) {
            errors.put("from", "The from field must match the format Y-m-d.");
        }
        if (to != null && !Rules.isDate(to)) {
            errors.put("to", "The to field must match the format Y-m-d.");
        } else if (to != null && from != null && Rules.isDate(from) && to.compareTo(from) < 0) {
            errors.put("to", "The to field must be a date after or equal to from.");
        }
        Long category = null;
        if (categoryText != null) {
            category = Rules.integer(categoryText);
            if (category == null) {
                errors.put("category", "The category field must be an integer.");
            } else if (category == 0 && !withType) {
                category = null;
            }
        }
        if (q != null && Rules.length(q) > 100) {
            errors.put("q", "The q field must not be greater than 100 characters.");
        }
        if (type != null && !type.equals("income") && !type.equals("expense")) {
            errors.put("type", "The selected type is invalid.");
        }
        return errors.size() > before ? NONE : new TransactionFilters(type, from, to, category, q);
    }
}
