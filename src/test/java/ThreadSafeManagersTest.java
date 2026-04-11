import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadSafeManagersTest {

    @Test
    void userManager_parallelAdd_noDuplicatesOrLoss() throws Exception {
        UserManager userManager = new UserManager();
        int total = 100;
        runParallel(total, i -> {
            String username = "u" + i;
            userManager.add(User.validate(username, "User " + i, username + "@example.com"));
        });

        assertEquals(total, userManager.count());
        assertEquals(total, userManager.findByFilterParallel(u -> u.username().startsWith("u")).size());
    }

    @Test
    void roleManager_parallelAddAndFilterParallel_consistent() throws Exception {
        RoleManager roleManager = new RoleManager();
        int total = 80;
        runParallel(total, i -> roleManager.add(new Role("role_" + i, "desc")));

        assertEquals(total, roleManager.count());
        assertEquals(total, roleManager.findByFilterParallel(r -> r.getName().startsWith("role_")).size());
    }

    @Test
    void assignmentManager_parallelAddAndFilterParallel_consistent() throws Exception {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        rm.setAssignmentManager(am);

        int total = 60;
        for (int i = 0; i < total; i++) {
            User user = User.validate("usr" + i, "User " + i, "usr" + i + "@example.com");
            Role role = new Role("role" + i, "desc");
            um.add(user);
            rm.add(role);
        }

        runParallel(total, i -> {
            User user = um.findByUsername("usr" + i).orElseThrow();
            Role role = rm.findByName("role" + i).orElseThrow();
            am.add(new PermanentAssignment(user, role, AssignmentMetadata.now("test", "parallel")));
        });

        assertEquals(total, am.count());
        assertEquals(total, am.findByFilterParallel(a -> a.isActive()).size());

        Set<String> ids = ConcurrentHashMap.newKeySet();
        am.findAll().forEach(a -> ids.add(a.assignmentId()));
        assertEquals(total, ids.size());
    }

    private static void runParallel(int total, ThrowingIntConsumer action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(16, total));
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);

        for (int i = 0; i < total; i++) {
            int idx = i;
            executor.submit(() -> {
                try {
                    start.await();
                    action.accept(idx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @FunctionalInterface
    interface ThrowingIntConsumer {
        void accept(int value) throws Exception;
    }
}
