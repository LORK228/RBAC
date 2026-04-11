import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGeneratorTest {

    @Test
    void generateUserReport_parallel_containsUsersInSortedOrder() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        rm.setAssignmentManager(am);

        User bob = User.validate("bob", "Bob", "bob@example.com");
        User alice = User.validate("alice", "Alice", "alice@example.com");
        um.add(bob);
        um.add(alice);

        Role admin = new Role("Admin", "full");
        rm.add(admin);
        am.add(new PermanentAssignment(alice, admin, AssignmentMetadata.now("system", "seed")));

        ReportGenerator generator = new ReportGenerator();
        String report = generator.generateUserReport(um, am);

        assertTrue(report.contains("alice"));
        assertTrue(report.contains("bob"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.indexOf("alice") < report.indexOf("bob"));
    }

    @Test
    void generatePermissionMatrix_parallel_containsResourcesAndPermissions() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        rm.setAssignmentManager(am);

        User user = User.validate("zoe", "Zoe", "zoe@example.com");
        um.add(user);

        Role role = new Role("Operator", "ops");
        role.addPermission(new Permission("READ", "users", "read users"));
        role.addPermission(new Permission("WRITE", "roles", "write roles"));
        rm.add(role);

        am.add(new PermanentAssignment(user, role, AssignmentMetadata.now("system", "seed")));

        ReportGenerator generator = new ReportGenerator();
        String matrix = generator.generatePermissionMatrix(um, am);

        assertTrue(matrix.contains("zoe"));
        assertTrue(matrix.contains("users"));
        assertTrue(matrix.contains("roles"));
        assertTrue(matrix.contains("READ"));
        assertTrue(matrix.contains("WRITE"));
    }
}
