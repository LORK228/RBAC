import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AssignmentFilters
{
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static AssignmentFilter byUser(User user)
    {
        return assignment -> assignment.user().equals(user);
    }
    public static AssignmentFilter byUsername(String username)
    {
        return assignment -> assignment.user().username().equals(username);
    }
    public static AssignmentFilter byRole(Role role)
    {
        return assignment -> assignment.role().equals(role);
    }
    public static AssignmentFilter byRoleName(String roleName)
    {
        return assignment -> assignment.role().getName().equals(roleName);
    }
    public static AssignmentFilter activeOnly()
    {
        return RoleAssignment::isActive;
    }
    public static AssignmentFilter inactiveOnly()
    {
        return assignment -> !assignment.isActive();
    }
    public static AssignmentFilter byType(String type)
    {
        return assignment -> assignment.assignmentType().equals(type);
    }
    public static AssignmentFilter assignedBy(String username)
    {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }
    public static AssignmentFilter assignedAfter(String date)
    {
        LocalDateTime targetDate = parseDate(date);
        return assignment -> {
            try {
                LocalDateTime assignedDate = LocalDateTime.parse(
                        assignment.metadata().assignedAt(),
                        ISO_FORMATTER
                );
                return assignedDate.isAfter(targetDate);
            } catch (Exception e) {
                return false;
            }
        };
    }
    public static AssignmentFilter expiringBefore(String date)
    {
        LocalDateTime targetDate = parseDate(date);
        return assignment -> {
            // Проверяем, что это временное назначение
            if (!assignment.assignmentType().equals("TEMPORARY")) {
                return false;
            }

            try {
                // Пытаемся привести к TemporaryAssignment
                if (assignment instanceof TemporaryAssignment temp) {
                    LocalDateTime expiresAt = temp.getExpiresAt();
                    return expiresAt.isBefore(targetDate);
                }
            } catch (Exception e) {
                // Игнорируем ошибки
            }
            return false;
        };
    }
    private static LocalDateTime parseDate(String dateString)
    {
        try {
            // Пытаемся распарсить как ISO дату со временем
            return LocalDateTime.parse(dateString, ISO_FORMATTER);
        } catch (Exception e1) {
            try {
                // Пытаемся распарсить как дату без времени и добавляем 00:00:00
                return LocalDateTime.parse(dateString + "T00:00:00", ISO_FORMATTER);
            } catch (Exception e2) {
                throw new IllegalArgumentException(
                        "Invalid date format. Use yyyy-MM-dd or ISO format (yyyy-MM-ddTHH:mm:ss)",
                        e2
                );
            }
        }
    }
}
