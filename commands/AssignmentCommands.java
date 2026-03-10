import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AssignmentCommands {
    public static void register(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }
            List<Role> roles = system.getRoleManager().findAll().stream()
                    .sorted(Comparator.comparing(Role::getName))
                    .toList();
            if (roles.isEmpty()) {
                ConsoleUtils.printError("No roles available");
                return;
            }
            Role role = ConsoleUtils.promptChoice(scanner, "Select role", roles);
            String type = ConsoleUtils.promptChoice(scanner, "Assignment type", List.of("PERMANENT", "TEMPORARY"));
            String reason = ConsoleUtils.promptString(scanner, "Reason", false);
            AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

            RoleAssignment assignment;
            if ("TEMPORARY".equals(type)) {
                String expires = ConsoleUtils.promptString(scanner, "Expiration date (YYYY-MM-DD or ISO)", true);
                assignment = new TemporaryAssignment(userOpt.get(), role, metadata, expires, false);
            } else {
                assignment = new PermanentAssignment(userOpt.get(), role, metadata);
            }

            system.getAssignmentManager().add(assignment);
            ConsoleUtils.printSuccess("Role assigned: " + assignment.assignmentId());
        });

        parser.registerCommand("revoke-role", "Revoke user's active role assignment", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }
            List<RoleAssignment> active = system.getAssignmentManager().findByUser(userOpt.get()).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();
            if (active.isEmpty()) {
                ConsoleUtils.printError("No active assignments");
                return;
            }
            RoleAssignment selected = ConsoleUtils.promptChoice(scanner, "Select assignment to revoke", active);
            system.getAssignmentManager().revokeAssignment(selected.assignmentId());
            ConsoleUtils.printSuccess("Assignment revoked");
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) ->
                CommandSupport.printAssignmentsTable(system.getAssignmentManager().findAll()));

        parser.registerCommand("assignment-list-user", "List assignments by user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }
            CommandSupport.printAssignmentsTable(system.getAssignmentManager().findByUser(userOpt.get()));
        });

        parser.registerCommand("assignment-list-role", "List users assigned to role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }
            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(roleOpt.get()).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();
            List<String[]> rows = assignments.stream()
                    .map(a -> new String[]{a.user().username(), a.assignmentId(), a.assignmentType()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"User", "Assignment ID", "Type"}, rows));
        });

        parser.registerCommand("assignment-active", "List active assignments", (scanner, system) ->
                CommandSupport.printAssignmentsTable(system.getAssignmentManager().getActiveAssignments()));

        parser.registerCommand("assignment-expired", "List expired temporary assignments", (scanner, system) ->
                CommandSupport.printAssignmentsTable(new ArrayList<>(system.getAssignmentManager().getExpiredAssignments())));

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            String mode = ConsoleUtils.promptChoice(scanner, "Find assignment by", List.of("Assignment ID", "Username + Role"));
            String assignmentId;
            if ("Assignment ID".equals(mode)) {
                assignmentId = ConsoleUtils.promptString(scanner, "Assignment ID", true);
            } else {
                String username = ConsoleUtils.promptString(scanner, "Username", true);
                String roleName = ConsoleUtils.promptString(scanner, "Role name", true);
                Optional<User> userOpt = system.getUserManager().findByUsername(username);
                Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);
                if (userOpt.isEmpty() || roleOpt.isEmpty()) {
                    ConsoleUtils.printError("User or role not found");
                    return;
                }
                assignmentId = system.getAssignmentManager().findByUser(userOpt.get()).stream()
                        .filter(a -> a.role().getId().equals(roleOpt.get().getId()))
                        .map(RoleAssignment::assignmentId)
                        .findFirst()
                        .orElse(null);
                if (assignmentId == null) {
                    ConsoleUtils.printError("Assignment not found");
                    return;
                }
            }

            String newDate = ConsoleUtils.promptString(scanner, "New expiration date", true);
            system.getAssignmentManager().extendTemporaryAssignment(assignmentId, newDate);
            ConsoleUtils.printSuccess("Assignment extended");
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, system) -> {
            List<String> options = List.of(
                    "By user",
                    "By role",
                    "By type",
                    "By status",
                    "Assigned after date",
                    "Expiring before date"
            );
            String selected = ConsoleUtils.promptChoice(scanner, "Select filter", options);
            List<RoleAssignment> result;

            switch (selected) {
                case "By user" -> {
                    String username = ConsoleUtils.promptString(scanner, "Username", true);
                    result = system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username));
                }
                case "By role" -> {
                    String roleName = ConsoleUtils.promptString(scanner, "Role name", true);
                    result = system.getAssignmentManager().findByFilter(AssignmentFilters.byRoleName(roleName));
                }
                case "By type" -> {
                    String type = ConsoleUtils.promptChoice(scanner, "Type", List.of("PERMANENT", "TEMPORARY"));
                    result = system.getAssignmentManager().findByFilter(AssignmentFilters.byType(type));
                }
                case "By status" -> {
                    String status = ConsoleUtils.promptChoice(scanner, "Status", List.of("ACTIVE", "INACTIVE"));
                    result = "ACTIVE".equals(status)
                            ? system.getAssignmentManager().findByFilter(AssignmentFilters.activeOnly())
                            : system.getAssignmentManager().findByFilter(AssignmentFilters.inactiveOnly());
                }
                case "Assigned after date" -> {
                    String date = ConsoleUtils.promptString(scanner, "Date (YYYY-MM-DDTHH:mm:ss)", true);
                    result = system.getAssignmentManager().findByFilter(AssignmentFilters.assignedAfter(date));
                }
                default -> {
                    String date = ConsoleUtils.promptString(scanner, "Date (YYYY-MM-DDTHH:mm:ss)", true);
                    result = system.getAssignmentManager().findByFilter(AssignmentFilters.expiringBefore(date));
                }
            }

            CommandSupport.printAssignmentsTable(result);
        });
    }
}
