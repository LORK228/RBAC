import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RoleCommands {
    public static void register(CommandParser parser) {
        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            List<String[]> rows = system.getRoleManager().findAll().stream()
                    .sorted(Comparator.comparing(Role::getName))
                    .map(r -> new String[]{r.getName(), String.valueOf(r.getPermissions().size()), r.getId()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Role", "Permissions", "ID"}, rows));
        });

        parser.registerCommand("role-create", "Create role and optionally add permissions", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name", true);
            String description = ConsoleUtils.promptString(scanner, "Role description", true);
            Role role = new Role(name, description);

            while (ConsoleUtils.promptYesNo(scanner, "Add permission to this role?")) {
                Permission permission = CommandSupport.promptPermission(scanner);
                role.addPermission(permission);
            }

            system.getRoleManager().add(role);
            ConsoleUtils.printSuccess("Role created");
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }
            System.out.println(roleOpt.get().format());
        });

        parser.registerCommand("role-update", "Update role name and description", (scanner, system) -> {
            String currentName = ConsoleUtils.promptString(scanner, "Current role name", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(currentName);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }
            Role role = roleOpt.get();
            String newName = ConsoleUtils.promptString(scanner, "New role name", false);
            String newDescription = ConsoleUtils.promptString(scanner, "New description", false);
            if (newName.isEmpty()) newName = role.getName();
            if (newDescription.isEmpty()) newDescription = role.getDescription();
            system.getRoleManager().updateRole(currentName, newName, newDescription);
            ConsoleUtils.printSuccess("Role updated");
        });

        parser.registerCommand("role-delete", "Delete role with assignment check", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }
            Role role = roleOpt.get();
            List<RoleAssignment> activeAssignments = system.getAssignmentManager().findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();
            if (!activeAssignments.isEmpty()) {
                ConsoleUtils.printError("Role has active assignments");
                List<String[]> rows = activeAssignments.stream()
                        .map(a -> new String[]{a.user().username(), a.assignmentId(), a.assignmentType()})
                        .toList();
                System.out.println(FormatUtils.formatTable(new String[]{"User", "Assignment ID", "Type"}, rows));
            }
            if (!ConsoleUtils.promptYesNo(scanner, "Confirm role deletion?")) {
                ConsoleUtils.printSuccess("Canceled");
                return;
            }
            system.getRoleManager().remove(role);
            ConsoleUtils.printSuccess("Role deleted");
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name", true);
            Permission permission = CommandSupport.promptPermission(scanner);
            system.getRoleManager().addPermissionToRole(roleName, permission);
            ConsoleUtils.printSuccess("Permission added");
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }
            Role role = roleOpt.get();
            List<Permission> permissions = new ArrayList<>(role.getPermissions());
            permissions.sort(Comparator.comparing(Permission::resource).thenComparing(Permission::name));
            if (permissions.isEmpty()) {
                ConsoleUtils.printError("Role has no permissions");
                return;
            }
            Permission selected = ConsoleUtils.promptChoice(scanner, "Select permission", permissions);
            system.getRoleManager().removePermissionFromRole(roleName, selected);
            ConsoleUtils.printSuccess("Permission removed");
        });

        parser.registerCommand("role-search", "Search roles by name/permission/count", (scanner, system) -> {
            List<String> options = List.of("Name contains", "Has permission", "Min permission count");
            String selected = ConsoleUtils.promptChoice(scanner, "Select search type", options);

            List<Role> result;
            switch (selected) {
                case "Name contains" -> {
                    String value = ConsoleUtils.promptString(scanner, "Name contains", true).toLowerCase();
                    result = system.getRoleManager().findAll().stream()
                            .filter(r -> r.getName().toLowerCase().contains(value))
                            .toList();
                }
                case "Has permission" -> {
                    String name = ConsoleUtils.promptString(scanner, "Permission name", true);
                    String resource = ConsoleUtils.promptString(scanner, "Resource", true);
                    result = system.getRoleManager().findRolesWithPermission(name, resource);
                }
                default -> {
                    int min = ConsoleUtils.promptInt(scanner, "Min permission count", 0, 1000);
                    result = system.getRoleManager().findAll().stream()
                            .filter(r -> r.getPermissions().size() >= min)
                            .toList();
                }
            }

            List<String[]> rows = result.stream()
                    .map(r -> new String[]{r.getName(), String.valueOf(r.getPermissions().size()), r.getId()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Role", "Permissions", "ID"}, rows));
        });
    }
}
