package id.its.tracker.service;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.Map;
import java.util.regex.Pattern;

public final class Passwords {

    private static final int COST = 12;
    private static final Pattern UPPER = Pattern.compile("\\p{Lu}");
    private static final Pattern LOWER = Pattern.compile("\\p{Ll}");
    private static final Pattern DIGIT = Pattern.compile("\\p{N}");
    private static final Pattern SYMBOL = Pattern.compile("[^\\p{L}\\p{N}]");

    private Passwords() {
    }

    public static String hash(String password) {
        return BCrypt.with(BCrypt.Version.VERSION_2Y).hashToString(COST, password.toCharArray());
    }

    public static boolean verify(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        return BCrypt.verifyer().verify(password.toCharArray(), hash.toCharArray()).verified;
    }

    public static boolean checkPolicy(String password, String field, Map<String, String> errors) {
        String message = null;
        if (password.codePointCount(0, password.length()) < 12) {
            message = "The password field must be at least 12 characters.";
        } else if (!UPPER.matcher(password).find() || !LOWER.matcher(password).find()) {
            message = "The password field must contain at least one uppercase and one lowercase letter.";
        } else if (!DIGIT.matcher(password).find()) {
            message = "The password field must contain at least one number.";
        } else if (!SYMBOL.matcher(password).find()) {
            message = "The password field must contain at least one symbol.";
        }
        if (message != null) {
            errors.putIfAbsent(field, message.replace("password field", field.replace('_', ' ') + " field"));
            return false;
        }
        return true;
    }
}
