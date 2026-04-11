import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncFeaturesTest {

    @Test
    void auditLog_queueWorker_persistsEntries() {
        AuditLog log = new AuditLog();
        try {
            log.log("CREATE_USER", "system", "alice", "created");
            log.log("CREATE_ROLE", "system", "admin", "created");

            assertTrue(log.getAll().size() >= 2);
            assertEquals(1, log.getByAction("CREATE_USER").size());
        } finally {
            log.close();
        }
    }

    @Test
    void rbacSystem_asyncSave_completesAndCreatesFile() throws Exception {
        try (RBACSystem system = new RBACSystem()) {
            system.initialize();
            Path temp = Files.createTempFile("rbac-save-", ".txt");
            Future<?> future = system.saveSystemAsync(temp.toString());
            future.get(5, TimeUnit.SECONDS);

            assertTrue(Files.exists(temp));
            assertTrue(Files.size(temp) > 0);
        }
    }

    @Test
    void rbacSystem_asyncUserReport_completes() throws Exception {
        try (RBACSystem system = new RBACSystem()) {
            system.initialize();
            Future<?> future = system.generateUsersReportAsync();
            future.get(5, TimeUnit.SECONDS);
            assertTrue(true);
        }
    }
}
