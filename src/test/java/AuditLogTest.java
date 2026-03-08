import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AuditLogTest {

    @Test
    public void testLogAndFilters() {
        AuditLog log = new AuditLog();

        log.log("CREATE_USER", "system", "alice", "created");
        log.log("CREATE_ROLE", "system", "admin", "created");
        log.log("ASSIGN_ROLE", "admin", "assign_1", "assigned");

        assertEquals(3, log.getAll().size());
        assertEquals(2, log.getByPerformer("system").size());
        assertEquals(1, log.getByAction("ASSIGN_ROLE").size());
    }

    @Test
    public void testSaveToFile() throws Exception {
        AuditLog log = new AuditLog();
        log.log("CREATE_USER", "system", "alice", "created");

        Path file = Files.createTempFile("audit-log-", ".txt");
        log.saveToFile(file.toString());

        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.size() >= 1);
        String content = String.join("\n", lines);
        assertTrue(content.contains("CREATE_USER"));
    }

    @Test
    public void testManagersWriteAuditEntries() {
        AuditLog log = new AuditLog();
        UserManager um = new UserManager(log);
        RoleManager rm = new RoleManager(log);
        AssignmentManager am = new AssignmentManager(um, rm, log);
        rm.setAssignmentManager(am);

        User user = TestDataFactory.createUser("alex");
        Role role = TestDataFactory.createRole("operator");

        um.add(user);
        rm.add(role);

        PermanentAssignment pa = new PermanentAssignment(user, role, AssignmentMetadata.now("admin", "manual assign"));
        am.add(pa);
        am.revokeAssignment(pa.assignmentId());

        rm.remove(role);
        um.remove(user);

        List<AuditEntry> all = log.getAll();
        assertTrue(all.stream().anyMatch(e -> e.action().equals("CREATE_USER")));
        assertTrue(all.stream().anyMatch(e -> e.action().equals("DELETE_USER")));
        assertTrue(all.stream().anyMatch(e -> e.action().equals("CREATE_ROLE")));
        assertTrue(all.stream().anyMatch(e -> e.action().equals("DELETE_ROLE")));
        assertTrue(all.stream().anyMatch(e -> e.action().equals("ASSIGN_ROLE")));
        assertTrue(all.stream().anyMatch(e -> e.action().equals("REVOKE_ROLE")));
    }
}
