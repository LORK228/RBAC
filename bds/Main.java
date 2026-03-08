import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        AuditLog auditLog = new AuditLog();
        ReportGenerator reportGenerator = new ReportGenerator();
        UserManager userManager = new UserManager(auditLog);
        RoleManager roleManager = new RoleManager(auditLog);
        AssignmentManager assignmentManager = new AssignmentManager(userManager, roleManager, auditLog);
        roleManager.setAssignmentManager(assignmentManager);

        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();
            if (line == null) {
                break;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String command = trimmed.split("\\s+")[0];

            try {
                switch (command) {
                    case "create-user" -> {
                        ConsoleUtils.printSection("Create User");
                        String username = ConsoleUtils.promptString(scanner, "Username", true);
                        String fullName = ConsoleUtils.promptString(scanner, "Full name", true);
                        String email = ConsoleUtils.promptString(scanner, "Email", true);
                        User user = User.validate(username, fullName, email);
                        userManager.add(user);
                        ConsoleUtils.printSuccess("User created: " + user.username());
                    }
                    case "delete-user" -> {
                        List<User> users = userManager.findAll().stream()
                                .sorted(Comparator.comparing(User::username))
                                .collect(Collectors.toList());
                        if (users.isEmpty()) {
                            ConsoleUtils.printError("No users found");
                            continue;
                        }
                        ConsoleUtils.printSection("Delete User");
                        User selected = ConsoleUtils.promptChoice(scanner, "Select user", users);
                        boolean confirm = ConsoleUtils.promptYesNo(scanner, "Delete user '" + selected.username() + "'?");
                        if (!confirm) {
                            ConsoleUtils.printSuccess("Canceled");
                            continue;
                        }
                        boolean removed = userManager.remove(selected);
                        ConsoleUtils.printSuccess(removed ? "User deleted" : "User not deleted");
                    }
                    case "create-role" -> {
                        ConsoleUtils.printSection("Create Role");
                        String roleName = ConsoleUtils.promptString(scanner, "Role name", true);
                        String description = ConsoleUtils.promptString(scanner, "Description", true);
                        Role role = new Role(roleName, description);
                        roleManager.add(role);
                        ConsoleUtils.printSuccess("Role created: " + role.getName());
                    }
                    case "delete-role" -> {
                        List<Role> roles = roleManager.findAll().stream()
                                .sorted(Comparator.comparing(Role::getName))
                                .collect(Collectors.toList());
                        if (roles.isEmpty()) {
                            ConsoleUtils.printError("No roles found");
                            continue;
                        }
                        ConsoleUtils.printSection("Delete Role");
                        Role selected = ConsoleUtils.promptChoice(scanner, "Select role", roles);
                        boolean confirm = ConsoleUtils.promptYesNo(scanner, "Delete role '" + selected.getName() + "'?");
                        if (!confirm) {
                            ConsoleUtils.printSuccess("Canceled");
                            continue;
                        }
                        boolean removed = roleManager.remove(selected);
                        ConsoleUtils.printSuccess(removed ? "Role deleted" : "Role not deleted");
                    }
                    case "assign-role" -> {
                        List<User> users = userManager.findAll().stream()
                                .sorted(Comparator.comparing(User::username))
                                .collect(Collectors.toList());
                        List<Role> roles = roleManager.findAll().stream()
                                .sorted(Comparator.comparing(Role::getName))
                                .collect(Collectors.toList());
                        if (users.isEmpty()) {
                            ConsoleUtils.printError("No users found");
                            continue;
                        }
                        if (roles.isEmpty()) {
                            ConsoleUtils.printError("No roles found");
                            continue;
                        }
                        ConsoleUtils.printSection("Assign Role");
                        User selectedUser = ConsoleUtils.promptChoice(scanner, "Select user", users);
                        Role selectedRole = ConsoleUtils.promptChoice(scanner, "Select role", roles);
                        String assignedBy = ConsoleUtils.promptString(scanner, "Assigned by", true);
                        AssignmentMetadata metadata = AssignmentMetadata.now(assignedBy, "manual");
                        PermanentAssignment assignment = new PermanentAssignment(selectedUser, selectedRole, metadata);
                        assignmentManager.add(assignment);
                        ConsoleUtils.printSuccess("Role assigned. Assignment ID: " + assignment.assignmentId());
                    }
                    case "revoke-role" -> {
                        List<RoleAssignment> assignments = new ArrayList<>(assignmentManager.findAll());
                        if (assignments.isEmpty()) {
                            ConsoleUtils.printError("No assignments found");
                            continue;
                        }
                        ConsoleUtils.printSection("Revoke Role");
                        RoleAssignment selected = ConsoleUtils.promptChoice(scanner, "Select assignment", assignments);
                        boolean confirm = ConsoleUtils.promptYesNo(scanner, "Revoke assignment '" + selected.assignmentId() + "'?");
                        if (!confirm) {
                            ConsoleUtils.printSuccess("Canceled");
                            continue;
                        }
                        assignmentManager.revokeAssignment(selected.assignmentId());
                        ConsoleUtils.printSuccess("Role revoked");
                    }
                    case "report-users" -> {
                        String report = reportGenerator.generateUserReport(userManager, assignmentManager);
                        System.out.println(report);
                        boolean shouldSave = ConsoleUtils.promptYesNo(scanner, "Save report to file?");
                        if (shouldSave) {
                            String filename = ConsoleUtils.promptString(scanner, "Filename", true);
                            reportGenerator.exportToFile(report, filename);
                            ConsoleUtils.printSuccess("User report saved to " + filename);
                        } else {
                            ConsoleUtils.printSuccess("Report shown in console");
                        }
                    }
                    case "report-roles" -> {
                        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);
                        System.out.println(report);
                        boolean shouldSave = ConsoleUtils.promptYesNo(scanner, "Save report to file?");
                        if (shouldSave) {
                            String filename = ConsoleUtils.promptString(scanner, "Filename", true);
                            reportGenerator.exportToFile(report, filename);
                            ConsoleUtils.printSuccess("Role report saved to " + filename);
                        } else {
                            ConsoleUtils.printSuccess("Report shown in console");
                        }
                    }
                    case "report-matrix" -> {
                        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);
                        System.out.println(report);
                        boolean shouldSave = ConsoleUtils.promptYesNo(scanner, "Save report to file?");
                        if (shouldSave) {
                            String filename = ConsoleUtils.promptString(scanner, "Filename", true);
                            reportGenerator.exportToFile(report, filename);
                            ConsoleUtils.printSuccess("Permission matrix saved to " + filename);
                        } else {
                            ConsoleUtils.printSuccess("Report shown in console");
                        }
                    }
                    case "audit-log" -> auditLog.printLog();
                    case "help" -> printHelp();
                    case "exit" -> {
                        return;
                    }
                    default -> System.out.println("Unknown command. Type 'help'");
                }
            } catch (Exception ex) {
                ConsoleUtils.printError(ex.getMessage());
            }
        }
    }

    private static void printHelp() {
        ConsoleUtils.printSection("Commands");
        List<String[]> rows = List.of(
                new String[]{"create-user", "Create user (wizard)"},
                new String[]{"delete-user", "Delete user (wizard)"},
                new String[]{"create-role", "Create role (wizard)"},
                new String[]{"delete-role", "Delete role (wizard)"},
                new String[]{"assign-role", "Assign role to user (wizard)"},
                new String[]{"revoke-role", "Revoke role assignment (wizard)"},
                new String[]{"report-users", "User report"},
                new String[]{"report-roles", "Role report"},
                new String[]{"report-matrix", "Permission matrix"},
                new String[]{"audit-log", "Show audit log"},
                new String[]{"help", "Show help"},
                new String[]{"exit", "Exit application"}
        );
        System.out.println(FormatUtils.formatTable(new String[]{"Command", "Description"}, rows));
    }
}
