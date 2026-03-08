import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private LocalDateTime expiresAt;
    private boolean autoRenew;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter READABLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               LocalDateTime expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }

        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAtString, boolean autoRenew) {
        this(user, role, metadata, parseExpirationString(expiresAtString), autoRenew);
    }

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               LocalDateTime expiresAt) {
        this(user, role, metadata, expiresAt, false);
    }

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAtString) {
        this(user, role, metadata, expiresAtString, false);
    }

    @Override
    public boolean isActive() {
        return LocalDateTime.now().isBefore(expiresAt);
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public void extend(String newExpirationDate) {
        LocalDateTime newExpiry = parseExpirationString(newExpirationDate);
        if (DateUtils.isBefore(newExpiry.toLocalDate().toString(), DateUtils.getCurrentDate())) {
            throw new IllegalArgumentException("New expiration date must be in the future");
        }
        if (newExpiry.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New expiration date must be in the future");
        }

        this.expiresAt = newExpiry;
    }

    public void extend(LocalDateTime newExpirationDate) {
        Objects.requireNonNull(newExpirationDate, "newExpirationDate must not be null");

        if (newExpirationDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New expiration date must be in the future");
        }

        this.expiresAt = newExpirationDate;
    }

    public void extendByDays(long days) {
        this.expiresAt = expiresAt.plusDays(days);
    }

    public void extendByHours(long hours) {
        this.expiresAt = expiresAt.plusHours(hours);
    }

    public long getRemainingDays() {
        Duration d = Duration.between(LocalDateTime.now(), expiresAt);
        return d.isNegative() ? -1 : d.toDays();
    }

    public long getRemainingHours() {
        Duration d = Duration.between(LocalDateTime.now(), expiresAt);
        return d.isNegative() ? -1 : d.toHours();
    }

    
    public long getRemainingMinutes() {
        Duration d = Duration.between(LocalDateTime.now(), expiresAt);
        return d.isNegative() ? -1 : d.toMinutes();
    }

    public String getTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        Duration d = Duration.between(now, expiresAt);

        if (d.isNegative() || d.isZero()) {
            return "EXPIRED";
        }

        long days = d.toDays();
        long hours = d.minusDays(days).toHours();
        long minutes = d.minusDays(days).minusHours(hours).toMinutes();

        if (days > 0) {
            return String.format("%d days %d hours", days, hours);
        } else if (hours > 0) {
            return String.format("%d hours %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder();

        LocalDateTime now = LocalDateTime.now();
        String assignedAtFormatted = formatDateTime(metadata.assignedAt());
        sb.append(String.format("[%s] %s assigned to %s by %s at %s\n",
                assignmentType(),
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                assignedAtFormatted));

        String reasonStr = metadata.reason() != null && !metadata.reason().isEmpty()
                ? metadata.reason()
                : "No reason provided";
        sb.append(String.format("Reason: %s\n", reasonStr));

        Duration d = Duration.between(now, expiresAt);
        String expiresAtFormatted = formatDateTime(expiresAt);
        String timeRemaining;
        if (d.isNegative() || d.isZero()) {
            timeRemaining = "EXPIRED";
        } else {
            long days = d.toDays();
            long hours = d.minusDays(days).toHours();
            timeRemaining = (days > 0) ? String.format("%d days %d hours", days, hours)
                    : String.format("%d hours %d minutes", d.toHours(), d.minusHours(d.toHours()).toMinutes());
        }
        String relativeDate = DateUtils.formatRelativeTime(expiresAt.toLocalDate().toString());
        sb.append(String.format("Expires: %s (%s remaining, %s)\n", expiresAtFormatted, timeRemaining, relativeDate));

        String status = d.isNegative() || d.isZero() ? "EXPIRED" : "ACTIVE";
        if (autoRenew && d.isNegative()) {
            status = "EXPIRED (AUTO_RENEW enabled)";
        }
        sb.append(String.format("Status: %s", status));

        return sb.toString();
    }

    private String formatDateTime(String isoDateTime) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(isoDateTime, ISO_FORMATTER);
            return dateTime.format(READABLE_FORMATTER);
        } catch (Exception e) {
            return isoDateTime;
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt.format(READABLE_FORMATTER);
    }

    private static LocalDateTime parseExpirationString(String value) {
        ValidationUtils.requireNonEmpty(value, "expiresAt");
        String trimmed = value.trim();
        if (trimmed.contains("T")) {
            return LocalDateTime.parse(trimmed, ISO_FORMATTER);
        }
        return LocalDateTime.parse(trimmed + "T23:59:59", ISO_FORMATTER);
    }
}
