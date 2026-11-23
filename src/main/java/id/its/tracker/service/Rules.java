package id.its.tracker.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class Rules {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private Rules() {
    }

    static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    static boolean isDate(String value) {
        try {
            LocalDate.parse(value, DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isMonth(String value) {
        return value.matches("[0-9]{4}-(0[1-9]|1[0-2])");
    }

    static boolean isTime(String value) {
        try {
            LocalTime.parse(value, TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static Long integer(String value) {
        if (value == null || !value.matches("[+-]?(0|[1-9][0-9]*)")) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
