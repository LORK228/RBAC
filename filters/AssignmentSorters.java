import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AssignmentSorters
{
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static Comparator<RoleAssignment> byUsername()
    {
        return Comparator.comparing(roleAssignment -> roleAssignment.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName()
    {
        return Comparator.comparing(roleAssignment -> roleAssignment.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate()
    {
        return Comparator.comparing(a -> LocalDateTime.parse(a.metadata().assignedAt(), ISO_FORMATTER));
    }
}
