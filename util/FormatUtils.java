import java.util.List;

public class FormatUtils {

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("headers must not be empty");
        }

        List<String[]> safeRows = rows == null ? List.of() : rows;
        int cols = headers.length;
        int[] widths = new int[cols];

        for (int i = 0; i < cols; i++) {
            widths[i] = safe(headers[i]).length();
        }

        for (String[] row : safeRows) {
            for (int i = 0; i < cols; i++) {
                String cell = getCell(row, i);
                if (cell.length() > widths[i]) {
                    widths[i] = cell.length();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        String border = buildBorder(widths);
        sb.append(border).append('\n');
        sb.append(buildRow(headers, widths)).append('\n');
        sb.append(border).append('\n');

        for (String[] row : safeRows) {
            String[] cells = new String[cols];
            for (int i = 0; i < cols; i++) {
                cells[i] = getCell(row, i);
            }
            sb.append(buildRow(cells, widths)).append('\n');
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatBox(String text) {
        String safeText = text == null ? "" : text;
        String[] lines = safeText.split("\\R", -1);
        int max = 0;
        for (String line : lines) {
            if (line.length() > max) {
                max = line.length();
            }
        }

        StringBuilder sb = new StringBuilder();
        String border = "+" + "-".repeat(max + 2) + "+";
        sb.append(border).append('\n');
        for (String line : lines) {
            sb.append(String.format("| %s |%n", padRight(line, max)));
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatHeader(String text) {
        String title = safe(text).toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append(title).append('\n');
        sb.append("=".repeat(Math.max(3, title.length())));
        return sb.toString();
    }

    public static String truncate(String text, int maxLength) {
        String safeText = safe(text);
        if (maxLength <= 0) {
            return "";
        }
        if (safeText.length() <= maxLength) {
            return safeText;
        }
        if (maxLength <= 3) {
            return ".".repeat(maxLength);
        }
        return safeText.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        String safeText = safe(text);
        if (safeText.length() >= length) {
            return safeText;
        }
        return safeText + " ".repeat(length - safeText.length());
    }

    public static String padLeft(String text, int length) {
        String safeText = safe(text);
        if (safeText.length() >= length) {
            return safeText;
        }
        return " ".repeat(length - safeText.length()) + safeText;
    }

    private static String buildBorder(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        return sb.toString();
    }

    private static String buildRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < widths.length; i++) {
            sb.append(" ")
                    .append(padRight(getCell(values, i), widths[i]))
                    .append(" |");
        }
        return sb.toString();
    }

    private static String getCell(String[] row, int idx) {
        if (row == null || idx < 0 || idx >= row.length || row[idx] == null) {
            return "";
        }
        return row[idx];
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}
