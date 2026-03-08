import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FMT);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FMT);
    }

    public static boolean isBefore(String date1, String date2) {
        String d1 = normalizeDate(date1);
        String d2 = normalizeDate(date2);
        return d1.compareTo(d2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        String d1 = normalizeDate(date1);
        String d2 = normalizeDate(date2);
        return d1.compareTo(d2) > 0;
    }

    public static String addDays(String date, int days) {
        String normalized = normalizeDate(date);
        LocalDate parsed = LocalDate.parse(normalized, DATE_FMT);
        return parsed.plusDays(days).format(DATE_FMT);
    }

    public static String formatRelativeTime(String date) {
        String normalized = normalizeDate(date);
        LocalDate target = LocalDate.parse(normalized, DATE_FMT);
        LocalDate now = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(now, target);
        if (diff == 0) {
            return "today";
        }
        if (diff > 0) {
            return String.format("in %d days", diff);
        }
        return String.format("%d days ago", -diff);
    }

    private static String normalizeDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("date must not be empty");
        }
        String trimmed = date.trim();
        if (trimmed.length() >= 10) {
            return trimmed.substring(0, 10);
        }
        throw new IllegalArgumentException("date must be in format YYYY-MM-DD");
    }
}
