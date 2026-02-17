import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public AssignmentMetadata {
        Objects.requireNonNull(assignedBy, "assignedBy must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");

        if (assignedBy.isEmpty()) {
            throw new IllegalArgumentException("assignedBy must not be empty");
        }
        if (assignedAt.isEmpty()) {
            throw new IllegalArgumentException("assignedAt must not be empty");
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(ISO_FORMATTER);
        return new AssignmentMetadata(assignedBy, formattedDateTime, reason);
    }


    public String format() {
        String reasonStr = reason != null && !reason.isEmpty() ? reason : "No reason provided";
        return String.format("Assigned by: %s at %s | Reason: %s",
                assignedBy, assignedAt, reasonStr);
    }

    @Override
    public String toString() {
        return String.format("[%s by %s]", assignedAt, assignedBy);
    }
}