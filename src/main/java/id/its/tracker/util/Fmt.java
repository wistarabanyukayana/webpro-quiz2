package id.its.tracker.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private Fmt() {
    }

    public static String money(long amount) {
        String digits = String.format(Locale.ROOT, "%,d", Math.abs(amount)).replace(',', '.');
        return (amount < 0 ? "-" : "") + "Rp " + digits;
    }

    public static String date(String isoDate) {
        return isoDate == null ? "" : LocalDate.parse(isoDate).format(DATE);
    }
}
