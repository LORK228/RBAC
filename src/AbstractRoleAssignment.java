import java.util.UUID;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractRoleAssignment implements RoleAssignment {

    protected String assignmentId;
    protected User user;
    protected Role role;
    protected AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");

        this.assignmentId = "assign_" + UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    // ==================== Геттеры ====================

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) obj;
        return assignmentId.equals(that.assignmentId);
    }

    @Override
    public int hashCode() {
        return assignmentId.hashCode();
    }


    public String summary() {
        StringBuilder sb = new StringBuilder();

        // Строка 1: [ТИП] роль назначена пользователю admin в дату
        String assignedAtFormatted = formatAssignedAt(metadata.assignedAt());
        sb.append(String.format("[%s] %s assigned to %s by %s at %s\n",
                assignmentType(),
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                assignedAtFormatted));

        // Строка 2: Причина назначения
        String reasonStr = metadata.reason() != null && !metadata.reason().isEmpty()
                ? metadata.reason()
                : "No reason provided";
        sb.append(String.format("Reason: %s\n", reasonStr));

        // Строка 3: Статус
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        sb.append(String.format("Status: %s", status));

        return sb.toString();
    }

    private String formatAssignedAt(String isoDateTime) {
        try {
            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
            DateTimeFormatter readableFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(isoDateTime, isoFormatter);
            return dateTime.format(readableFormatter);
        } catch (Exception e) {
            // Если парсинг не удался, возвращаем как есть
            return isoDateTime;
        }
    }

    @Override
    public String toString() {
        return String.format("%s{assignmentId='%s', user=%s, role=%s, type=%s, active=%s}",
                getClass().getSimpleName(),
                assignmentId,
                user.username(),
                role.getName(),
                assignmentType(),
                isActive());
    }

    @Override
    public String format() {
        return String.format(
                "Assignment [ID: %s]\n" +
                        "User: %s (%s)\n" +
                        "Role: %s\n" +
                        "Type: %s (Active: %s)\n" +
                        "Metadata: %s",
                assignmentId(),
                user().username(),
                user().fullName(),
                role().getName(),
                assignmentType(),
                isActive(),
                metadata().format()
        );
    }
}