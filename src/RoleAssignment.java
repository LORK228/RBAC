public interface RoleAssignment {
    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();
    default String format() {
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