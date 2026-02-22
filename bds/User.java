import java.text.MessageFormat;

public record User(String username, String fullName, String email) {
    public static User validate(String username, String fullName, String email) {
        if (username == null) throw new IllegalArgumentException("username must not be null");
        if (fullName == null) throw new IllegalArgumentException("fullName must not be null");
        if (email == null) throw new IllegalArgumentException("email must not be null");

        if (username.isEmpty()) throw new IllegalArgumentException("username must not be empty");
        if (fullName.isEmpty()) throw new IllegalArgumentException("fullName must not be empty");
        if (email.isEmpty()) throw new IllegalArgumentException("email must not be empty");

        if (!username.matches("^[A-Za-z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("username must be 3-20 characters and contain only Latin letters, digits and underscore");
        }

        int at = email.indexOf('@');
        if (at <= 0) throw new IllegalArgumentException("email must contain '@' and have non-empty local part");

        int dotAfterAt = email.indexOf('.', at + 1);
        if (dotAfterAt <= at + 1) throw new IllegalArgumentException("email must contain a '.' after '@' (e.g. user@example.com)");

        return new User(username, fullName, email);
    }
    public String format()
    {
        return MessageFormat.format("username {0} " + "{1}",fullName,email);
    }
}