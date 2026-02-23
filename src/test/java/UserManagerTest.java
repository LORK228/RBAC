import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class UserManagerTest {

    private UserManager userManager;

    @BeforeEach
    public void setup() {
        userManager = new UserManager();
    }

    @Test
    public void testAddValidUser_increasesCount() {
        User u = TestDataFactory.createUser("alice");
        userManager.add(u);
        assertEquals(1, userManager.count());
        Optional<User> found = userManager.findByUsername("alice");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().username());
    }

    @Test
    public void testAddNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> userManager.add(null));
    }

    @Test
    public void testAddDuplicateUsername_throws() {
        User u1 = TestDataFactory.createUser("bob");
        User u2 = TestDataFactory.createUser("bob");
        userManager.add(u1);
        assertThrows(IllegalArgumentException.class, () -> userManager.add(u2));
    }

    @Test
    public void testAddDuplicateEmail_throws() {
        User u1 = User.validate("charlie", "Charlie", "same@example.com");
        User u2 = User.validate("charlie2", "Charlie2", "same@example.com");
        userManager.add(u1);
        assertThrows(IllegalArgumentException.class, () -> userManager.add(u2));
    }

    @Test
    public void testRemoveExisting_returnsTrueAndDecreasesCount() {
        User u = TestDataFactory.createUser("dan");
        userManager.add(u);
        assertTrue(userManager.remove(u));
        assertEquals(0, userManager.count());
    }

    @Test
    public void testRemoveNull_returnsFalse() {
        assertFalse(userManager.remove(null));
    }

    @Test
    public void testFindById_nullOrEmpty_returnsEmpty() {
        assertTrue(userManager.findById(null).isEmpty());
        assertTrue(userManager.findById("").isEmpty());
    }

    @Test
    public void testFindByUsername_and_findByEmail() {
        User u = TestDataFactory.createUser("eve");
        userManager.add(u);
        assertTrue(userManager.findByUsername("eve").isPresent());
        assertTrue(userManager.findByEmail("eve@example.com").isPresent());
    }

    @Test
    public void testExists_nullOrEmpty_returnsFalse() {
        assertFalse(userManager.exists(null));
        assertFalse(userManager.exists(""));
    }

    @Test
    public void testUpdate_successfulUpdatesUser() {
        User u = TestDataFactory.createUser("frank");
        userManager.add(u);
        userManager.update("frank", "Frank Old", "franknew@example.com");
        Optional<User> updated = userManager.findByUsername("frank");
        assertTrue(updated.isPresent());
        assertEquals("franknew@example.com", updated.get().email());
        assertEquals("Frank Old", updated.get().fullName());
    }

    @Test
    public void testUpdate_nonExistingUser_throws() {
        assertThrows(IllegalArgumentException.class, () -> userManager.update("noone", "n", "n@e.com"));
    }

    @Test
    public void testCountByFilter_returnsCorrectNumber() {
        User a = TestDataFactory.createUser("u1");
        User b = TestDataFactory.createUser("u2");
        userManager.add(a);
        userManager.add(b);
        int cnt = userManager.countByFilter(user -> user.username().startsWith("u"));
        assertEquals(2, cnt);
    }
}
