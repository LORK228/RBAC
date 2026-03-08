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

        for (AuditEntry entry : entries) {
            System.out.printf("[%s] action=%s performer=%s target=%s details=%s%n",
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details());
        }
    }

    public void saveToFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("filename must not be empty");
        }

        List<String> lines = entries.stream()
                .map(e -> String.format("[%s] action=%s performer=%s target=%s details=%s",
                        e.timestamp(), e.action(), e.performer(), e.target(), e.details()))
                .collect(Collectors.toList());

        Path path = Path.of(filename.trim());
        try {
            Files.write(path, lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save audit log to file: " + filename, ex);
        }
    }
}
