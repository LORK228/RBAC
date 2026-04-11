import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RBACLoadTest {

    @Test
    void concurrentCreateUpdateAssignAndFilter_noCorruption() throws Exception {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        rm.setAssignmentManager(am);

        int threadCount = 8;
        int iterations = 40;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            int threadIndex = t;
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        String username = "usr_" + threadIndex + "_" + i;
                        String roleName = "role_" + threadIndex + "_" + i;

                        User user = User.validate(username, "User " + threadIndex + "-" + i, username + "@example.com");
                        Role role = new Role(roleName, "desc");
                        role.addPermission(new Permission("READ", "users", "read users"));

                        um.add(user);
                        rm.add(role);

                        um.update(username, "Updated " + username, username + "@example.com");

                        am.add(new PermanentAssignment(
                                user,
                                role,
                                AssignmentMetadata.now("load-test", "parallel")));

                        um.findByFilterParallel(u -> u.username().contains("usr_"));
                        rm.findByFilterParallel(r -> r.getName().contains("role_"));
                        am.findByFilterParallel(RoleAssignment::isActive);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));

        int expected = threadCount * iterations;
        assertEquals(expected, um.count());
        assertEquals(expected, rm.count());
        assertEquals(expected, am.count());

        Set<String> usernames = ConcurrentHashMap.newKeySet();
        um.findAll().forEach(u -> usernames.add(u.username()));
        assertEquals(expected, usernames.size());

        Set<String> assignmentIds = ConcurrentHashMap.newKeySet();
        am.findAll().forEach(a -> assignmentIds.add(a.assignmentId()));
        assertEquals(expected, assignmentIds.size());
    }
}
