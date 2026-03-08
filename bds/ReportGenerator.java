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
        sb.append(FormatUtils.formatHeader("User Report")).append("\n\n");

        List<User> users = userManager.findAll().stream()
                .sorted(Comparator.comparing(User::username))
                .collect(Collectors.toList());

        List<String[]> rows = new ArrayList<>();
        for (User user : users) {
            List<String> activeRoles = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getName())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            String rolesStr = activeRoles.isEmpty() ? "-" : String.join(", ", activeRoles);
            rows.add(new String[]{
                    FormatUtils.truncate(user.username(), 20),
                    FormatUtils.truncate(user.fullName(), 30),
                    FormatUtils.truncate(user.email(), 35),
                    FormatUtils.truncate(rolesStr, 40)
            });
        }

        sb.append(FormatUtils.formatTable(
                new String[]{"Username", "Full Name", "Email", "Roles"},
                rows
        ));
        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        if (roleManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers must not be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("Role Report")).append("\n\n");

        List<Role> roles = roleManager.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .collect(Collectors.toList());

        List<String[]> rows = new ArrayList<>();
        for (Role role : roles) {
            long userCount = assignmentManager.findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.user().username())
                    .distinct()
                    .count();

            rows.add(new String[]{
                    FormatUtils.truncate(role.getName(), 25),
                    FormatUtils.truncate(role.getDescription(), 45),
                    String.format("%d", userCount)
            });
        }

        sb.append(FormatUtils.formatTable(
                new String[]{"Role", "Description", "Users"},
                rows
        ));
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
        sb.append(FormatUtils.formatHeader("Permission Matrix")).append("\n\n");

        List<String> headersList = new ArrayList<>();
        headersList.add("User");
        for (String resource : resourceColumns) {
            headersList.add(FormatUtils.truncate(resource, 20));
        }
        String[] headers = headersList.toArray(new String[0]);

        List<String[]> rows = new ArrayList<>();

        for (User user : users) {
            List<String> row = new ArrayList<>();
            row.add(FormatUtils.truncate(user.username(), 20));

            Map<String, List<String>> byResource = new TreeMap<>();
            for (Permission permission : assignmentManager.getUserPermissions(user)) {
                byResource.computeIfAbsent(permission.resource(), k -> new ArrayList<>()).add(permission.name());
            }

            for (String resource : resourceColumns) {
                List<String> names = byResource.getOrDefault(resource, List.of());
                String cell = names.isEmpty() ? "-" : names.stream().distinct().sorted().collect(Collectors.joining(","));
                row.add(FormatUtils.truncate(cell, 30));
            }
            rows.add(row.toArray(new String[0]));
        }

        sb.append(FormatUtils.formatTable(headers, rows));
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
