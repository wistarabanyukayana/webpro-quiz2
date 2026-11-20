package id.its.tracker.service;

import id.its.tracker.dao.AccountDao;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AccountService {

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*");

    private AccountService() {
    }

    public static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    public static void checkIdentity(String username, String fullName, String email, long exceptId,
                                     Map<String, String> errors) {
        if (username == null) {
            errors.put("username", "The username field is required.");
        } else if (Rules.length(username) < 3) {
            errors.put("username", "The username field must be at least 3 characters.");
        } else if (Rules.length(username) > 255) {
            errors.put("username", "The username field must not be greater than 255 characters.");
        } else if (AccountDao.usernameTaken(username, exceptId)) {
            errors.put("username", "The username has already been taken.");
        }

        if (fullName == null) {
            errors.put("full_name", "The full name field is required.");
        } else if (Rules.length(fullName) > 255) {
            errors.put("full_name", "The full name field must not be greater than 255 characters.");
        }

        if (email == null) {
            errors.put("email", "The email field is required.");
        } else if (!EMAIL.matcher(email).matches()) {
            errors.put("email", "The email field must be a valid email address.");
        } else if (Rules.length(email) > 255) {
            errors.put("email", "The email field must not be greater than 255 characters.");
        } else if (AccountDao.emailTaken(email, exceptId)) {
            errors.put("email", "The email has already been taken.");
        }
    }

    public static void checkNewPassword(String password, String confirmation, String field, Map<String, String> errors) {
        String label = field.replace('_', ' ');
        if (password == null) {
            errors.put(field, "The " + label + " field is required.");
        } else if (!password.equals(confirmation)) {
            errors.put(field, "The " + label + " field confirmation does not match.");
        } else {
            Passwords.checkPolicy(password, field, errors);
        }
    }

}
