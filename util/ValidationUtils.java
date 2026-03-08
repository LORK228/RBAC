import java.util.Locale;
import java.util.regex.Pattern;

public class ValidationUtils
{
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?)?$");

    public static boolean isValidUsername(String username)
    {
        return username != null && !username.isEmpty() && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email)
    {
        return email != null && !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDate(String date)
    {
        return date != null && !date.isEmpty() && DATE_PATTERN.matcher(date).matches();
    }

    public static String normalizeString(String input)
    {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim().replaceAll("\\s+", " ");
        return trimmed.toLowerCase(Locale.ROOT);
    }

    public static void requireNonEmpty(String value, String fieldName)
    {
        String field = (fieldName == null || fieldName.isEmpty()) ? "value" : fieldName;
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
    }
}
