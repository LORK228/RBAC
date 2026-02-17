import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== User Validation Tests ===\n");

        try {
            var u = User.validate("john_doe", "John Doe", "john.doe@example.com");
            System.out.println("Created user: " + u);
            System.out.println(u.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("Validation failed for valid user: " + ex.getMessage());
        }

        try {
            User.validate("jo!n", "John Invalid", "john.invalid@example.com");
        } catch (IllegalArgumentException ex) {
            System.out.println("Expected failure (username chars): " + ex.getMessage());
        }

        try {
            User.validate("ab", "Too Short", "short@example.com");
        } catch (IllegalArgumentException ex) {
            System.out.println("Expected failure (username length): " + ex.getMessage());
        }

        try {
            User.validate("valid_user", "No Dot", "nodot@domain");
        } catch (IllegalArgumentException ex) {
            System.out.println("Expected failure (email format): " + ex.getMessage());
        }

        try {
            User.validate("", "", "");
        } catch (IllegalArgumentException ex) {
            System.out.println("Expected failure (empty fields): " + ex.getMessage());
        }

        System.out.println("\n=== Permission Tests ===\n");

        try {
            var p1 = new Permission("read", "Users", "Allows reading users");
            System.out.println(p1.format());
            System.out.println("matches READ/user: " + p1.matches("READ", "user"));
            System.out.println("matches REA/ser: " + p1.matches("REA", "ser"));
            System.out.println("matches X/ser: " + p1.matches("X", "ser"));
        } catch (IllegalArgumentException ex) {
            System.out.println("Permission creation failed: " + ex.getMessage());
        }

        
        try {
            new Permission("bad name", "res", "desc");
        } catch (IllegalArgumentException ex) {
            System.out.println("Expected failure (name contains spaces): " + ex.getMessage());
        }

        try {
            new Permission("OK", "res", "   ");
        } catch (IllegalArgumentException ex) {
            System.out.println("Expected failure (empty description): " + ex.getMessage());
        }

        System.out.println("\n=== PermanentAssignment Tests ===\n");

        User admin = User.validate("admin", "Administrator", "admin@example.com");
        User manager = User.validate("manager_user", "Manager", "manager@example.com");

        Role adminRole = new Role("Administrator", "Full system access");
        Role managerRole = new Role("Manager", "Manager access");

        AssignmentMetadata metadata1 = new AssignmentMetadata("admin", "2026-02-13T10:00:00", "Initial setup");
        PermanentAssignment permanent = new PermanentAssignment(admin, adminRole, metadata1);

        System.out.println("Permanent Assignment:");
        System.out.println(permanent.summary());
        System.out.println("\nIs active: " + permanent.isActive());
        System.out.println("Is revoked: " + permanent.isRevoked());

        System.out.println("\n--- After revoke ---");
        permanent.revoke();
        System.out.println("Is active: " + permanent.isActive());
        System.out.println("Is revoked: " + permanent.isRevoked());
        System.out.println(permanent.summary());

        System.out.println("\n=== TemporaryAssignment Tests ===\n");

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        AssignmentMetadata metadata2 = AssignmentMetadata.now("admin", "Project access");
        TemporaryAssignment temporary = new TemporaryAssignment(manager, managerRole, metadata2, expiresAt, false);

        System.out.println("Temporary Assignment:");
        System.out.println(temporary.summary());
        System.out.println("\nTime remaining: " + temporary.getTimeRemaining());
        System.out.println("Remaining days: " + temporary.getRemainingDays());
        System.out.println("Remaining hours: " + temporary.getRemainingHours());
        System.out.println("Is active: " + temporary.isActive());
        System.out.println("Is expired: " + temporary.isExpired());

        System.out.println("\n--- After extending by 30 days ---");
        temporary.extendByDays(30);
        System.out.println("Time remaining: " + temporary.getTimeRemaining());
        System.out.println("Remaining days: " + temporary.getRemainingDays());

        System.out.println("\n--- Temporary with auto-renew ---");
        LocalDateTime shortExpiry = LocalDateTime.now().plusHours(5);
        TemporaryAssignment tempAutoRenew = new TemporaryAssignment(
                manager,
                managerRole,
                metadata2,
                shortExpiry,
                true
        );
        System.out.println("Auto-renew enabled: " + tempAutoRenew.isAutoRenew());
        System.out.println(tempAutoRenew.summary());

        System.out.println("\n=== Tests finished ===");
    }
}
