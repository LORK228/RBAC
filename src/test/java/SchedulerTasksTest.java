import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerTasksTest {

    @Test
    void scheduledTask_marksExpiredTemporaryAssignmentsInactive_andLogsStats() throws Exception {
        try (RBACSystem system = new RBACSystem()) {
            User user = User.validate("tim", "Tim User", "tim@example.com");
            Role role = new Role("TempRole", "temp");

            system.getUserManager().add(user);
            system.getRoleManager().add(role);

            TemporaryAssignment assignment = new TemporaryAssignment(
                    user,
                    role,
                    AssignmentMetadata.now("test", "scheduler"),
                    LocalDateTime.now().plusSeconds(1),
                    false);
            system.getAssignmentManager().add(assignment);

            system.startMaintenanceScheduler(1);
            Thread.sleep(2500);

            assertTrue(assignment.isMarkedInactive());
            assertTrue(system.getAuditLog().getByAction("SCHEDULED_MAINTENANCE").size() >= 1);
        }
    }
}
