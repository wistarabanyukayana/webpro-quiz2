package id.its.tracker.util;

import java.util.StringJoiner;

public final class Csv {

    private Csv() {
    }

    // prefix ' so spreadsheets don't run formulas (csv injection)
    public static String row(Object... values) {
        StringJoiner line = new StringJoiner(",", "", "\n");
        for (Object value : values) {
            if (value == null) {
                line.add("");
                continue;
            }
            String text = value.toString();
            if (value instanceof String && !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
                text = "'" + text;
            }
            line.add(quote(text));
        }
        return line.toString();
    }

    private static String quote(String text) {
        for (char c : new char[] {',', '"', '\\', ' ', '\t', '\n', '\r'}) {
            if (text.indexOf(c) >= 0) {
                return '"' + text.replace("\"", "\"\"") + '"';
            }
        }
        return text;
    }
}
