import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        AuditLog auditLog = new AuditLog();
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

            String[] parts = trimmed.split("\\s+");
            String command = parts[0];

            try {
                switch (command) {
                    case "create-user" -> {
                        if (parts.length < 4) {
                            System.out.println("Usage: create-user <username> <fullName> <email>");
                            continue;
                        }
                        User user = User.validate(parts[1], parts[2], parts[3]);
                        userManager.add(user);
                        System.out.println("User created: " + user.username());
                    }
                    case "delete-user" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: delete-user <username>");
                            continue;
                        }
                        Optional<User> userOpt = userManager.findByUsername(parts[1]);
                        if (userOpt.isEmpty()) {
                            System.out.println("User not found");
                            continue;
                        }
                        boolean removed = userManager.remove(userOpt.get());
                        System.out.println(removed ? "User deleted" : "User not deleted");
                    }
                    case "create-role" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: create-role <name> <description>");
                            continue;
                        }
                        Role role = new Role(parts[1], parts[2]);
                        roleManager.add(role);
                        System.out.println("Role created: " + role.getName());
                    }
                    case "delete-role" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: delete-role <roleName>");
                            continue;
                        }
                        Optional<Role> roleOpt = roleManager.findByName(parts[1]);
                        if (roleOpt.isEmpty()) {
                            System.out.println("Role not found");
                            continue;
                        }
                        boolean removed = roleManager.remove(roleOpt.get());
                        System.out.println(removed ? "Role deleted" : "Role not deleted");
                    }
                    case "assign-role" -> {
                        if (parts.length < 4) {
                            System.out.println("Usage: assign-role <username> <roleName> <assignedBy>");
                            continue;
                        }
                        Optional<User> userOpt = userManager.findByUsername(parts[1]);
                        Optional<Role> roleOpt = roleManager.findByName(parts[2]);
                        if (userOpt.isEmpty()) {
                            System.out.println("User not found");
                            continue;
                        }
                        if (roleOpt.isEmpty()) {
                            System.out.println("Role not found");
                            continue;
                        }
                        AssignmentMetadata metadata = AssignmentMetadata.now(parts[3], "manual");
                        PermanentAssignment assignment = new PermanentAssignment(userOpt.get(), roleOpt.get(), metadata);
                        assignmentManager.add(assignment);
                        System.out.println("Role assigned. Assignment ID: " + assignment.assignmentId());
                    }
                    case "revoke-role" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: revoke-role <assignmentId>");
                            continue;
                        }
                        assignmentManager.revokeAssignment(parts[1]);
                        System.out.println("Role revoked");
                    }
                    case "audit-log" -> auditLog.printLog();
                    case "help" -> printHelp();
                    case "exit" -> {
                        return;
                    }
                    default -> System.out.println("Unknown command. Type 'help'");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  create-user <username> <fullName> <email>");
        System.out.println("  delete-user <username>");
        System.out.println("  create-role <name> <description>");
        System.out.println("  delete-role <roleName>");
        System.out.println("  assign-role <username> <roleName> <assignedBy>");
        System.out.println("  revoke-role <assignmentId>");
        System.out.println("  audit-log");
        System.out.println("  help");
        System.out.println("  exit");
    }
}
