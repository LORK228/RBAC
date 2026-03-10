import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserCommands {
    public static void register(CommandParser parser) {
        parser.registerCommand("user-list", "List users (optional filter: key=value)", (scanner, system) -> {
            List<User> users = new ArrayList<>(system.getUserManager().findAll());
            String args = parser.getLastArgs();
            if (!args.isEmpty()) {
                String[] kv = args.split("=", 2);
                String key = kv[0].trim().toLowerCase();
                String value = kv.length > 1 ? kv[1].trim().toLowerCase() : "";
                users = users.stream().filter(u -> switch (key) {
                    case "username" -> u.username().toLowerCase().contains(value);
                    case "email" -> u.email().toLowerCase().contains(value);
                    case "domain" -> u.email().toLowerCase().endsWith(value);
                    case "fullname" -> u.fullName().toLowerCase().contains(value);
                    default -> u.username().toLowerCase().contains(args.toLowerCase());
                }).collect(Collectors.toList());
            }
            users.sort(Comparator.comparing(User::username));
            List<String[]> rows = users.stream()
                    .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                    .collect(Collectors.toList());
            System.out.println(FormatUtils.formatHeader("Users"));
            System.out.println(FormatUtils.formatTable(new String[]{"Username", "Full Name", "Email"}, rows));
        });

        parser.registerCommand("user-create", "Create new user", (scanner, system) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Username", true);
                String fullName = ConsoleUtils.promptString(scanner, "Full name", true);
                String email = ConsoleUtils.promptString(scanner, "Email", true);
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                ConsoleUtils.printSuccess("User created: " + user.username());
            } catch (Exception ex) {
                ConsoleUtils.printError(ex.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user with roles and permissions", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }
            User user = userOpt.get();
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
            List<Role> roles = assignments.stream().filter(RoleAssignment::isActive).map(RoleAssignment::role).distinct().toList();
            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);

            System.out.println(FormatUtils.formatHeader("User Details"));
            System.out.println(FormatUtils.formatBox(
                    String.format("Username: %s%nFull Name: %s%nEmail: %s", user.username(), user.fullName(), user.email())));

            List<String[]> roleRows = roles.stream()
                    .map(r -> new String[]{r.getName(), r.getDescription()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Role", "Description"}, roleRows));

            List<String[]> permRows = permissions.stream()
                    .sorted(Comparator.comparing(Permission::resource).thenComparing(Permission::name))
                    .map(p -> new String[]{p.resource(), p.name(), p.description()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Resource", "Permission", "Description"}, permRows));
        });

        parser.registerCommand("user-update", "Update user full name and email", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            String fullName = ConsoleUtils.promptString(scanner, "New full name", true);
            String email = ConsoleUtils.promptString(scanner, "New email", true);
            system.getUserManager().update(username, fullName, email);
            ConsoleUtils.printSuccess("User updated");
        });

        parser.registerCommand("user-delete", "Delete user and all user assignments", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }
            String confirm = ConsoleUtils.promptString(scanner, "Type 'yes' to confirm", true);
            if (!"yes".equalsIgnoreCase(confirm)) {
                ConsoleUtils.printSuccess("Canceled");
                return;
            }

            User user = userOpt.get();
            List<RoleAssignment> assignments = new ArrayList<>(system.getAssignmentManager().findByUser(user));
            for (RoleAssignment a : assignments) {
                system.getAssignmentManager().remove(a);
            }
            system.getUserManager().remove(user);
            ConsoleUtils.printSuccess("User deleted");
        });

        parser.registerCommand("user-search", "Search users by filter", (scanner, system) -> {
            List<String> options = List.of(
                    "Username contains",
                    "Email contains",
                    "Email domain",
                    "Full name contains"
            );
            String selected = ConsoleUtils.promptChoice(scanner, "Select search type", options);
            String value = ConsoleUtils.promptString(scanner, "Search value", true).toLowerCase();
            List<User> all = system.getUserManager().findAll();
            List<User> result = all.stream().filter(u -> switch (selected) {
                case "Username contains" -> u.username().toLowerCase().contains(value);
                case "Email contains" -> u.email().toLowerCase().contains(value);
                case "Email domain" -> u.email().toLowerCase().endsWith(value);
                default -> u.fullName().toLowerCase().contains(value);
            }).sorted(Comparator.comparing(User::username)).toList();

            List<String[]> rows = result.stream()
                    .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Username", "Full Name", "Email"}, rows));
        });
    }
}
