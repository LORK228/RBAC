import java.text.MessageFormat;

public record User(String username, String fullName, String email) {
    public static User validate(String username, String fullName, String email) {
        String normalizedUsername = ValidationUtils.normalizeString(username);
        String normalizedEmail = ValidationUtils.normalizeString(email);
        String normalizedFullName = fullName == null ? null : fullName.trim().replaceAll("\\s+", " ");

        ValidationUtils.requireNonEmpty(normalizedUsername, "username");
        ValidationUtils.requireNonEmpty(normalizedFullName, "fullName");
        ValidationUtils.requireNonEmpty(normalizedEmail, "email");

        if (!ValidationUtils.isValidUsername(normalizedUsername)) {
            throw new IllegalArgumentException("username must be 3-20 characters and contain only Latin letters, digits and underscore");
        }

        if (!ValidationUtils.isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("email must match format user@example.com");
        }

        return new User(normalizedUsername, normalizedFullName, normalizedEmail);
    }
    public String format()
    {
        return MessageFormat.format("username {0} " + "{1}",fullName,email);
    }
}
