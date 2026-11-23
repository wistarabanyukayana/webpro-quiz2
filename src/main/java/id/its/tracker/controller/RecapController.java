package id.its.tracker.controller;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import id.its.tracker.App;
import id.its.tracker.dao.TransactionDao;
import id.its.tracker.model.CategoryTotal;
import id.its.tracker.model.Transaction;
import id.its.tracker.service.Rules;
import id.its.tracker.service.TransactionFilters;
import id.its.tracker.util.Csv;
import id.its.tracker.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

@WebServlet({"/recap", "/recap/csv", "/recap/report", "/recap/pdf"})
public class RecapController extends HttpServlet {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    static final List<String> CHART_COLORS = List.of("--swatch-blue", "--swatch-peach", "--swatch-mauve",
            "--swatch-teal", "--swatch-yellow", "--swatch-pink", "--swatch-sapphire", "--swatch-lavender");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, String> input = Web.old(req, "mode", "month", "from", "to", "year");
        validate(input, errors);
        String path = Web.path(req);
        if (!errors.isEmpty() && !path.equals("/recap")) {
            res.sendRedirect("/recap" + (req.getQueryString() == null ? "" : "?" + req.getQueryString()));
            return;
        }
        if (!errors.isEmpty()) {
            input.replaceAll((k, v) -> null);
        }

        long accountId = Web.account(req).id();
        String mode = input.get("mode") == null ? "month" : input.get("mode");
        YearMonth now = YearMonth.from(App.today());
        String month = or(input.get("month"), now.toString());
        String from = or(input.get("from"), now.toString());
        String to = or(input.get("to"), now.toString());
        String year = or(input.get("year"), String.valueOf(now.getYear()));

        LocalDate start;
        LocalDate end;
        String label;
        switch (mode) {
            case "year" -> {
                start = LocalDate.of(Integer.parseInt(year), 1, 1);
                end = start.withDayOfYear(start.lengthOfYear());
                label = year;
            }
            case "range" -> {
                start = YearMonth.parse(from).atDay(1);
                end = YearMonth.parse(to).atEndOfMonth();
                label = YearMonth.parse(from).format(LABEL) + " to " + YearMonth.parse(to).format(LABEL);
            }
            default -> {
                start = YearMonth.parse(month).atDay(1);
                end = YearMonth.parse(month).atEndOfMonth();
                label = YearMonth.parse(month).format(LABEL);
            }
        }

        TransactionFilters period = new TransactionFilters(null, start.toString(), end.toString(), null, null);
        long income = TransactionDao.sum(accountId, period.withType("income"));
        long expense = TransactionDao.sum(accountId, period.withType("expense"));
        List<CategoryTotal> totals = TransactionDao.totalsByCategory(accountId, period);
        List<Transaction> rows = TransactionDao.all(accountId, period);
        String slug = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");

        switch (path) {
            case "/recap/csv" -> {
                res.setContentType("text/csv; charset=UTF-8");
                res.setHeader("Content-Disposition", "attachment; filename=\"recap-" + slug + ".csv\"");
                PrintWriter out = res.getWriter();
                out.print(Csv.row("date", "time", "type", "category", "amount", "notes"));
                for (Transaction t : rows) {
                    out.print(Csv.row(t.date(), t.time(), t.type(), category(t), t.amount(), t.notes()));
                }
                return;
            }
            case "/recap/report", "/recap/pdf" -> {
                req.setAttribute("periodLabel", label);
                req.setAttribute("income", income);
                req.setAttribute("expense", expense);
                req.setAttribute("net", income - expense);
                req.setAttribute("incomeTotals", ofType(totals, "income"));
                req.setAttribute("expenseTotals", ofType(totals, "expense"));
                req.setAttribute("rows", rows);
                if (path.equals("/recap/report")) {
                    req.getRequestDispatcher("/WEB-INF/views/recap/report.jsp").forward(req, res);
                } else {
                    pdf(req, res, slug);
                }
                return;
            }
            default -> {
            }
        }

        List<CategoryTotal> incomeTotals = ofType(totals, "income");
        List<CategoryTotal> expenseTotals = ofType(totals, "expense");
        req.setAttribute("mode", mode);
        req.setAttribute("month", month);
        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("year", year);
        req.setAttribute("periodLabel", label);
        req.setAttribute("income", income);
        req.setAttribute("expense", expense);
        req.setAttribute("net", income - expense);
        req.setAttribute("incomeTotals", incomeTotals);
        req.setAttribute("expenseTotals", expenseTotals);
        // same scale for both charts
        req.setAttribute("chartMax", totals.stream().mapToLong(CategoryTotal::total).max().orElse(0));
        req.setAttribute("chartColors", CHART_COLORS);
        req.setAttribute("incomeWheel", wheel(incomeTotals));
        req.setAttribute("expenseWheel", wheel(expenseTotals));
        req.setAttribute("exportQuery", switch (mode) {
            case "year" -> "mode=year&year=" + year;
            case "range" -> "mode=range&from=" + from + "&to=" + to;
            default -> "mode=month&month=" + month;
        });
        if (!errors.isEmpty()) {
            res.setStatus(422);
            req.setAttribute("errors", errors);
        }
        Web.render(req, res, "recap");
    }

    private static void validate(Map<String, String> input, Map<String, String> errors) {
        String mode = input.get("mode");
        if (mode != null && !List.of("month", "range", "year").contains(mode)) {
            errors.put("mode", "The selected mode is invalid.");
        }
        for (String field : List.of("month", "from", "to")) {
            if (input.get(field) != null && !Rules.isMonth(input.get(field))) {
                errors.put(field, "The " + field + " field must match the format Y-m.");
            }
        }
        String from = input.get("from");
        String to = input.get("to");
        if (to != null && !errors.containsKey("to") && (from == null || !Rules.isMonth(from) || to.compareTo(from) < 0)) {
            errors.put("to", "The to field must be a date after or equal to from.");
        }
        if (input.get("year") != null && !input.get("year").matches("[0-9]{4}")) {
            errors.put("year", "The year field must match the format Y.");
        }
    }

    private static String or(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String category(Transaction t) {
        return t.category() == null ? "Uncategorized" : t.category();
    }

    private static List<CategoryTotal> ofType(List<CategoryTotal> totals, String type) {
        return totals.stream().filter(t -> t.type().equals(type)).toList();
    }

    private static String wheel(List<CategoryTotal> items) {
        long sum = items.stream().mapToLong(CategoryTotal::total).sum();
        StringJoiner segments = new StringJoiner(", ", "conic-gradient(", ")");
        double position = 0;
        for (int i = 0; i < items.size(); i++) {
            double start = position;
            position += sum > 0 ? items.get(i).total() * 100.0 / sum : 0;
            segments.add(String.format(Locale.ROOT, "var(%s) %.3f%% %.3f%%",
                    CHART_COLORS.get(i % CHART_COLORS.size()), start, position));
        }
        return items.isEmpty() ? "none" : segments.toString();
    }

    private static void pdf(HttpServletRequest req, HttpServletResponse res, String slug)
            throws ServletException, IOException {
        CharArrayWriter html = new CharArrayWriter();
        PrintWriter capture = new PrintWriter(html);
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(res) {
            @Override
            public PrintWriter getWriter() {
                return capture;
            }
        };
        req.getRequestDispatcher("/WEB-INF/views/recap/report.jsp").include(req, wrapper);
        capture.flush();

        res.setContentType("application/pdf");
        res.setHeader("Content-Disposition", "attachment; filename=recap-" + slug + ".pdf");
        try (OutputStream out = res.getOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            // built-in Helvetica only, characters outside latin-1 print as #
            builder.withHtmlContent(html.toString(), null);
            builder.toStream(out);
            builder.run();
        }
    }
}
