import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AuditLog implements AutoCloseable {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_DATE_TIME;
    private final List<AuditEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final Thread worker;
    private volatile boolean running = true;

    public AuditLog() {
        this.worker = new Thread(this::runWorker, "audit-log-worker");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(TS_FORMAT);
        String normalizedAction = action == null ? "" : action.trim();
        String normalizedPerformer = performer == null ? "" : performer.trim();
        String normalizedTarget = target == null ? "" : target.trim();
        String normalizedDetails = details == null ? "" : details.trim();
        queue.offer(new AuditEntry(timestamp, normalizedAction, normalizedPerformer, normalizedTarget, normalizedDetails));
    }

    public List<AuditEntry> getAll() {
        drainQueue();
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null || performer.trim().isEmpty()) {
            return List.of();
        }
        final String normalized = performer.trim();
        return getAll().stream()
                .filter(e -> e.performer().equals(normalized))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return List.of();
        }
        final String normalized = action.trim();
        return getAll().stream()
                .filter(e -> e.action().equals(normalized))
                .collect(Collectors.toList());
    }

    public void printLog() {
        List<AuditEntry> snapshot = getAll();
        if (snapshot.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        List<String[]> rows = toRows(snapshot);
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

        List<AuditEntry> snapshot = getAll();
        String content = FormatUtils.formatHeader("Audit Log") + System.lineSeparator()
                + System.lineSeparator()
                + FormatUtils.formatTable(
                new String[]{"Timestamp", "Action", "Performer", "Target", "Details"},
                toRows(snapshot)
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

    private void runWorker() {
        while (running || !queue.isEmpty()) {
            try {
                AuditEntry entry = queue.poll(200, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    entries.add(entry);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        drainQueue();
    }

    private void drainQueue() {
        AuditEntry entry;
        while ((entry = queue.poll()) != null) {
            entries.add(entry);
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

    @Override
    public void close() {
        running = false;
        worker.interrupt();
        try {
            worker.join(1000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        drainQueue();
    }
}
