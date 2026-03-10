import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionCommands {
    public static void register(CommandParser parser) {
        parser.registerCommand("permissions-user", "List all permissions for user (grouped by resource)", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(userOpt.get());
            Map<String, List<Permission>> grouped = CommandSupport.groupPermissionsByResource(permissions);

            List<String[]> rows = new ArrayList<>();
            for (Map.Entry<String, List<Permission>> e : grouped.entrySet()) {
                String perms = e.getValue().stream()
                        .map(Permission::name)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                rows.add(new String[]{e.getKey(), perms});
            }
            rows.sort(Comparator.comparing(a -> a[0]));
            System.out.println(FormatUtils.formatTable(new String[]{"Resource", "Permissions"}, rows));
        });

        parser.registerCommand("permissions-check", "Check if user has specific permission", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            String permissionName = ConsoleUtils.promptString(scanner, "Permission name", true);
            String resource = ConsoleUtils.promptString(scanner, "Resource", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }
            User user = userOpt.get();
            boolean has = system.getAssignmentManager().userHasPermission(user, permissionName, resource);
            if (!has) {
                ConsoleUtils.printError("Permission not granted");
                return;
            }

            List<String> roles = system.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::role)
                    .filter(r -> r.hasPermission(permissionName, resource))
                    .map(Role::getName)
                    .distinct()
                    .sorted()
                    .toList();

            ConsoleUtils.printSuccess("Permission granted");
            System.out.println("From roles: " + String.join(", ", roles));
        });
    }
}
