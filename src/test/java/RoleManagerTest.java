import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RoleManagerTest {
    private RoleManager roleManager;

    @BeforeEach
    public void setup() {
        roleManager = new RoleManager();
    }

    @Test
    public void testAddValidRole_increasesCount() {
        Role r = TestDataFactory.createRole("admin");
        roleManager.add(r);
        assertEquals(1, roleManager.count());
        assertTrue(roleManager.findByName("admin").isPresent());
    }

    @Test
    public void testAddNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(null));
    }

    @Test
    public void testAddDuplicateName_throws() {
        Role r1 = TestDataFactory.createRole("role1");
        Role r2 = new Role(r1.getName(), "desc");
        roleManager.add(r1);
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(r2));
    }

    @Test
    public void testRemoveRole_withoutAssignments_successful() {
        Role r = TestDataFactory.createRole("roleToRemove");
        roleManager.add(r);
        assertTrue(roleManager.remove(r));
        assertEquals(0, roleManager.count());
    }

    @Test
    public void testRemoveRole_withActiveAssignment_throws() {
        Role r = TestDataFactory.createRole("assignedRole");
        roleManager.add(r);
        UserManager um = new UserManager();
        User u = TestDataFactory.createUser("greg");
        um.add(u);
        AssignmentManager am = new AssignmentManager(um, roleManager);
        roleManager.setAssignmentManager(am);
        PermanentAssignment pa = TestDataFactory.createPermanentAssignment(u, r);
        am.add(pa);

        assertThrows(IllegalArgumentException.class, () -> roleManager.remove(r));
    }

    @Test
    public void testAddPermissionAndFindRolesWithPermission() {
        Role r = TestDataFactory.createRole("permRole");
        Permission p = TestDataFactory.createPermission("READ", "resource1");
        roleManager.add(r);
        roleManager.addPermissionToRole(r.getName(), p);
        List<Role> found = roleManager.findRolesWithPermission("READ", "resource1");
        assertEquals(1, found.size());
        assertEquals(r, found.get(0));
    }

    @Test
    public void testFindById_nullEmpty_returnsEmpty() {
        assertTrue(roleManager.findById(null).isEmpty());
        assertTrue(roleManager.findById("").isEmpty());
    }

    @Test
    public void testExists_nullEmpty_returnsFalse() {
        assertFalse(roleManager.exists(null));
        assertFalse(roleManager.exists(""));
    }
}
