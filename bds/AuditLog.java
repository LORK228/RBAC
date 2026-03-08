import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_DATE_TIME;
    private final List<AuditEntry> entries = new ArrayList<>();

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(TS_FORMAT);
        String normalizedAction = action == null ? "" : action.trim();
        String normalizedPerformer = performer == null ? "" : performer.trim();
        String normalizedTarget = target == null ? "" : target.trim();
        String normalizedDetails = details == null ? "" : details.trim();
        entries.add(new AuditEntry(timestamp, normalizedAction, normalizedPerformer, normalizedTarget, normalizedDetails));
    }

    public List<AuditEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null || performer.trim().isEmpty()) {
            return List.of();
        }
        final String normalized = performer.trim();
        return entries.stream()
                .filter(e -> e.performer().equals(normalized))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return List.of();
        }
        final String normalized = action.trim();
        return entries.stream()
                .filter(e -> e.action().equals(normalized))
                .collect(Collectors.toList());
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        List<String[]> rows = toRows(entries);
        String table = FormatUtils.formatTable(
                new String[]{"Timestamp", "Action", "Performer", "Target", "Details"},
                rows
        );
        System.out.println(FormatUtils.formatHeader("Audit Log"));
        System.out.println(table);
    }

    public void saveToFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("filename must not be empty");
        }

        String content = FormatUtils.formatHeader("Audit Log") + System.lineSeparator()
                + System.lineSeparator()
                + FormatUtils.formatTable(
                new String[]{"Timestamp", "Action", "Performer", "Target", "Details"},
                toRows(entries)
        );

        Path path = Path.of(filename.trim());
        try {
            Files.write(path, List.of(content),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save audit log to file: " + filename, ex);
        }
    }

    private List<String[]> toRows(List<AuditEntry> source) {
        return source.stream()
                .map(e -> new String[]{
                        FormatUtils.truncate(e.timestamp(), 26),
                        FormatUtils.truncate(e.action(), 20),
                        FormatUtils.truncate(e.performer(), 20),
                        FormatUtils.truncate(e.target(), 28),
                        FormatUtils.truncate(e.details(), 40)
                })
                .collect(Collectors.toList());
    }
}
