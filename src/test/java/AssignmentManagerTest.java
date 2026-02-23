import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class AssignmentManagerTest {

    private UserManager um;
    private RoleManager rm;
    private AssignmentManager am;

    @BeforeEach
    public void setup() {
        um = new UserManager();
        rm = new RoleManager();
        am = new AssignmentManager(um, rm);
    }

    @Test
    public void testConstructor_nullManagers_throws() {
        assertThrows(IllegalArgumentException.class, () -> new AssignmentManager(null, rm));
        assertThrows(IllegalArgumentException.class, () -> new AssignmentManager(um, null));
    }

    @Test
    public void testAddAndFindAndCount() {
        User u = TestDataFactory.createUser("hank");
        Role r = TestDataFactory.createRole("roleA");
        um.add(u);
        rm.add(r);

        PermanentAssignment pa = TestDataFactory.createPermanentAssignment(u, r);
        am.add(pa);

        assertEquals(1, am.count());
        assertTrue(am.findById(pa.assignmentId()).isPresent());
        List<RoleAssignment> byUser = am.findByUser(u);
        assertEquals(1, byUser.size());
    }

    @Test
    public void testAddDuplicateAssignmentId_throws() {
        User u = TestDataFactory.createUser("ivy");
        Role r = TestDataFactory.createRole("roleB");
        um.add(u);
        rm.add(r);
        PermanentAssignment pa1 = TestDataFactory.createPermanentAssignment(u, r);
        PermanentAssignment pa2 = new PermanentAssignment(u, r, pa1.metadata());
        // force same id
        pa2.assignmentId = pa1.assignmentId();

        am.add(pa1);
        assertThrows(IllegalArgumentException.class, () -> am.add(pa2));
    }

    @Test
    public void testAddUserDoesNotExist_throws() {
        User u = TestDataFactory.createUser("jack");
        Role r = TestDataFactory.createRole("roleC");
        rm.add(r);
        PermanentAssignment pa = TestDataFactory.createPermanentAssignment(u, r);
        assertThrows(IllegalArgumentException.class, () -> am.add(pa));
    }

    @Test
    public void testGetActiveAndExpiredAssignments() {
        User u = TestDataFactory.createUser("kate");
        Role r = TestDataFactory.createRole("roleD");
        um.add(u);
        rm.add(r);
        TemporaryAssignment tSoonExpiring = new TemporaryAssignment(
                u,
                r,
                TestDataFactory.nowMetadata("system"),
                java.time.LocalDateTime.now().plusSeconds(1)
        );
        am.add(tSoonExpiring);

        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for temporary assignment to expire");
        }

        Role activeRole = TestDataFactory.createRole("roleD_active");
        rm.add(activeRole);
        am.add(TestDataFactory.createPermanentAssignment(u, activeRole));

        List<RoleAssignment> active = am.getActiveAssignments();
        List<RoleAssignment> expired = am.getExpiredAssignments();

        assertTrue(active.stream().anyMatch(RoleAssignment::isActive));
        assertTrue(expired.size() >= 1);
    }

    @Test
    public void testUserHasRole_and_userHasPermission_and_getUserPermissions() {
        User u = TestDataFactory.createUser("leo");
        Role r = TestDataFactory.createRole("roleE");
        Permission p = TestDataFactory.createPermission("WRITE", "resX");
        r.addPermission(p);
        um.add(u);
        rm.add(r);

        PermanentAssignment pa = TestDataFactory.createPermanentAssignment(u, r);
        am.add(pa);

        assertTrue(am.userHasRole(u, r));
        assertTrue(am.userHasPermission(u, "WRITE", "resX"));
        Set<Permission> perms = am.getUserPermissions(u);
        assertTrue(perms.contains(p));
    }

    @Test
    public void testRevokeAssignment_permanent_revokedFlagTrue() {
        User u = TestDataFactory.createUser("mike");
        Role r = TestDataFactory.createRole("roleF");
        um.add(u);
        rm.add(r);
        PermanentAssignment pa = TestDataFactory.createPermanentAssignment(u, r);
        am.add(pa);
        am.revokeAssignment(pa.assignmentId());
        assertTrue(pa instanceof PermanentAssignment && ((PermanentAssignment) pa).isRevoked());
    }

    @Test
    public void testExtendTemporaryAssignment_success_updatesExpiry() {
        User u = TestDataFactory.createUser("nina");
        Role r = TestDataFactory.createRole("roleG");
        um.add(u);
        rm.add(r);
        TemporaryAssignment t = TestDataFactory.createTemporaryAssignment(u, r, 10);
        am.add(t);
        String newExp = TestDataFactory.isoMinutesFromNow(60);
        am.extendTemporaryAssignment(t.assignmentId(), newExp);
        assertTrue(t.getExpiresAt().isAfter(java.time.LocalDateTime.now().plusMinutes(30)));
    }
}
