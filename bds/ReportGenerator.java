import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        if (userManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers must not be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== User Report ===\n");

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .collect(Collectors.toList());

        for (User user : users) {
            List<String> activeRoles = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getName())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            String rolesStr = activeRoles.isEmpty() ? "-" : String.join(", ", activeRoles);
            sb.append(String.format("User: %s (%s)%n", user.username(), user.email()));
            sb.append(String.format("Roles: %s%n", rolesStr));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        if (roleManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers must not be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Role Report ===\n");

        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .collect(Collectors.toList());

        for (Role role : roles) {
            long userCount = assignmentManager.findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.user().username())
                    .distinct()
                    .count();

            sb.append(String.format("Role: %s | Users: %d%n", role.getName(), userCount));
        }

        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        if (userManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers must not be null");
        }

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .collect(Collectors.toList());

        Set<String> resources = new LinkedHashSet<>();
        for (User user : users) {
            for (Permission permission : assignmentManager.getUserPermissions(user)) {
                resources.add(permission.resource());
            }
        }

        List<String> resourceColumns = new ArrayList<>(resources);
        resourceColumns.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Permission Matrix ===\n");
        sb.append(String.format("%-20s", "User"));
        for (String resource : resourceColumns) {
            sb.append(String.format("| %-20s", resource));
        }
        sb.append("\n");

        for (User user : users) {
            sb.append(String.format("%-20s", user.username()));

            Map<String, List<String>> byResource = new TreeMap<>();
            for (Permission permission : assignmentManager.getUserPermissions(user)) {
                byResource.computeIfAbsent(permission.resource(), k -> new ArrayList<>()).add(permission.name());
            }

            for (String resource : resourceColumns) {
                List<String> names = byResource.getOrDefault(resource, List.of());
                String cell = names.isEmpty() ? "-" : names.stream().distinct().sorted().collect(Collectors.joining(","));
                sb.append(String.format("| %-20s", cell));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        if (report == null) {
            throw new IllegalArgumentException("report must not be null");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("filename must not be empty");
        }

        Path path = Path.of(filename.trim());
        try {
            Files.writeString(path, report);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to export report to file: " + filename, ex);
        }
    }
}
