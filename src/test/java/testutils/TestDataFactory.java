import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestDataFactory {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    public static User createUser(String username) {
        String normalized = normalizeUsername(username);
        return User.validate(normalized, "Full Name " + normalized, normalized + "@example.com");
    }

    public static Role createRole(String name) {
        return new Role(name, "Description for " + name);
    }

    public static Permission createPermission(String name, String resource) {
        return new Permission(name, resource, "perm " + name + " on " + resource);
    }

    public static AssignmentMetadata nowMetadata(String assignedBy) {
        return AssignmentMetadata.now(assignedBy, "test reason");
    }

    public static PermanentAssignment createPermanentAssignment(User user, Role role) {
        return new PermanentAssignment(user, role, nowMetadata("system"));
    }

    public static TemporaryAssignment createTemporaryAssignment(User user, Role role, long minutesFromNow) {
        LocalDateTime dt = LocalDateTime.now().plusMinutes(minutesFromNow);
        return new TemporaryAssignment(user, role, nowMetadata("system"), dt);
    }

    public static String isoMinutesFromNow(long minutes) {
        return LocalDateTime.now().plusMinutes(minutes).format(ISO);
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        if (username.length() >= 3) {
            return username;
        }
        StringBuilder sb = new StringBuilder(username);
        while (sb.length() < 3) {
            sb.append('_');
        }
        return sb.toString();
    }
}
