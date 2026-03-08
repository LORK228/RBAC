import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User>
{
    private Map<String, User> users = new HashMap<>();
    private AuditLog auditLog;

    public UserManager() {
        this(null);
    }

    public UserManager(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public void setAuditLog(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void add(User user)
    {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null");

        User validatedUser = User.validate(user.username(), user.fullName(), user.email());

        if (users.containsKey(validatedUser.username()))
            throw new IllegalArgumentException("User with username '" + validatedUser.username() + "' already exists");

        if (users.values().stream().anyMatch(u -> u.email().equals(validatedUser.email())))
            throw new IllegalArgumentException("User with email '" + validatedUser.email() + "' already exists");

        users.put(validatedUser.username(), validatedUser);
        if (auditLog != null) {
            auditLog.log("CREATE_USER", "system", validatedUser.username(),
                    "User created with email " + validatedUser.email());
        }
    }

    @Override
    public boolean remove(User user)
    {
        if (user == null)
            return false;

        boolean removed = users.remove(user.username()) != null;
        if (removed && auditLog != null) {
            auditLog.log("DELETE_USER", "system", user.username(), "User deleted");
        }
        return removed;
    }

    @Override
    public Optional<User> findById(String id)
    {
        if (id == null || id.isEmpty())
            return Optional.empty();

        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll()
    {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count()
    {
        return users.size();
    }

    @Override
    public void clear()
    {
        users.clear();
    }


    public Optional<User> findByUsername(String username)
    {
        String normalized = ValidationUtils.normalizeString(username);
        if (normalized == null || normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(normalized));
    }


    public Optional<User> findByEmail(String email)
    {
        String normalized = ValidationUtils.normalizeString(email);
        if (normalized == null || normalized.isEmpty()) {
            return Optional.empty();
        }
        return users.values().stream()
                .filter(user -> user.email().equals(normalized))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter)
    {
        if (filter == null) {
            return new ArrayList<>();
        }
        return users.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter)
    {
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username)
    {
        String normalized = ValidationUtils.normalizeString(username);
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        return users.containsKey(normalized);
    }

    public void update(String username, String newFullName, String newEmail)
    {
        String normalizedUsername = ValidationUtils.normalizeString(username);
        ValidationUtils.requireNonEmpty(normalizedUsername, "Username");

        User existingUser = users.get(normalizedUsername);
        if (existingUser == null)
            throw new IllegalArgumentException("User with username '" + normalizedUsername + "' not found");


        User validatedUser = User.validate(normalizedUsername, newFullName, newEmail);
        String normalizedEmail = validatedUser.email();

        if (users.values().stream().anyMatch(u -> u.email().equals(normalizedEmail)))
            throw new IllegalArgumentException("User with email '" + normalizedEmail + "' already exists");

        users.put(normalizedUsername, validatedUser);
    }

    public int countByFilter(UserFilter filter)
    {
        return (int) users.values().stream()
                .filter(filter::test)
                .count();
    }

    @Override
    public String toString()
    {
        return "UserManager{" +
                "users=" + users +
                ", total=" + users.size() +
                '}';
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof UserManager)) return false;

        UserManager that = (UserManager) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(users);
    }
}
