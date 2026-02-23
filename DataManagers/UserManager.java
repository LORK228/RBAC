import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User>
{
    private Map<String, User> users = new HashMap<>();

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
    }

    @Override
    public boolean remove(User user)
    {
        if (user == null)
            return false;

        return users.remove(user.username()) != null;
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
        if (username == null || username.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username));
    }


    public Optional<User> findByEmail(String email)
    {
        if (email == null || email.isEmpty()) {
            return Optional.empty();
        }
        return users.values().stream()
                .filter(user -> user.email().equals(email))
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
        if (username == null || username.isEmpty()) {
            return false;
        }
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail)
    {
        if (username == null || username.isEmpty())
            throw new IllegalArgumentException("Username cannot be null or empty");

        User existingUser = users.get(username);
        if (existingUser == null)
            throw new IllegalArgumentException("User with username '" + username + "' not found");


        User validatedUser = User.validate(username, newFullName, newEmail);

        if (users.values().stream().anyMatch(u -> u.email().equals(newEmail)))
            throw new IllegalArgumentException("User with email '" + newEmail + "' already exists");

        users.put(username, validatedUser);
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