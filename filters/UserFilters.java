public class UserFilters
{
    // Точное совпадение по username
    public static UserFilter byUsername(String username)
    {
        return user -> user.username().equals(username);
    }

    // Username содержит подстроку (игнорируя регистр)
    public static UserFilter byUsernameContains(String substring)
    {
        return user -> user.username().toLowerCase().contains(substring.toLowerCase());
    }

    // Точное совпадение по email
    public static UserFilter byEmail(String email)
    {
        return user -> user.email().equals(email);
    }

    // Email заканчивается на домен (например, "@company.com")
    public static UserFilter byEmailDomain(String domain)
    {
        return user -> user.email().endsWith(domain);
    }

    // Полное имя содержит подстроку
    public static UserFilter byFullNameContains(String substring)
    {
        return user -> user.fullName().toLowerCase().contains(substring.toLowerCase());
    }
}