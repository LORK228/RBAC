import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CommandSupport {
    public static Permission promptPermission(java.util.Scanner scanner) {
        String name = ConsoleUtils.promptString(scanner, "Permission name", true);
        String resource = ConsoleUtils.promptString(scanner, "Resource", true);
        String description = ConsoleUtils.promptString(scanner, "Description", true);
        return new Permission(name, resource, description);
    }

    public static void printAssignmentsTable(List<RoleAssignment> assignments) {
        List<String[]> rows = assignments.stream()
                .sorted(Comparator.comparing(RoleAssignment::assignmentId))
                .map(a -> new String[]{
                        a.assignmentId(),
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        a.isActive() ? "ACTIVE" : "INACTIVE",
                        a.metadata().assignedAt()
                })
                .toList();
        System.out.println(FormatUtils.formatTable(
                new String[]{"ID", "Username", "Role", "Type", "Status", "Assigned At"},
                rows
        ));
    }

    public static void saveSystem(RBACSystem system, String filename) {
        List<String> lines = new ArrayList<>();
        lines.add("[USERS]");
        for (User u : system.getUserManager().findAll()) {
            lines.add(String.join("|", u.username(), u.fullName(), u.email()));
        }

        lines.add("[ROLES]");
        for (Role r : system.getRoleManager().findAll()) {
            String perms = r.getPermissions().stream()
                    .map(p -> String.join(",", p.name(), p.resource(), p.description().replace("|", "/")))
                    .collect(Collectors.joining(";"));
            lines.add(String.join("|", r.getName(), r.getDescription().replace("|", "/"), perms));
        }

        lines.add("[ASSIGNMENTS]");
        for (RoleAssignment a : system.getAssignmentManager().findAll()) {
            String extra = "";
            if (a instanceof TemporaryAssignment t) {
                extra = t.getExpiresAt().toString();
            } else if (a instanceof PermanentAssignment p) {
                extra = String.valueOf(p.isRevoked());
            }
            lines.add(String.join("|",
                    a.assignmentType(),
                    a.user().username(),
                    a.role().getName(),
                    a.metadata().assignedBy(),
                    a.metadata().assignedAt(),
                    a.metadata().reason() == null ? "" : a.metadata().reason().replace("|", "/"),
                    extra));
        }

        try {
            Files.write(Path.of(filename), lines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save: " + e.getMessage(), e);
        }
    }

    public static void loadSystem(RBACSystem system, String filename) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load: " + e.getMessage(), e);
        }

        system.getAssignmentManager().clear();
        system.getRoleManager().clear();
        system.getUserManager().clear();

        String section = "";
        List<String[]> assignmentRows = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line;
                continue;
            }

            String[] parts = line.split("\\|", -1);
            switch (section) {
                case "[USERS]" -> {
                    if (parts.length >= 3) {
                        system.getUserManager().add(User.validate(parts[0], parts[1], parts[2]));
                    }
                }
                case "[ROLES]" -> {
                    if (parts.length >= 2) {
                        Role role = new Role(parts[0], parts[1]);
                        if (parts.length >= 3 && !parts[2].isEmpty()) {
                            String[] perms = parts[2].split(";");
                            for (String token : perms) {
                                String[] p = token.split(",", 3);
                                if (p.length == 3) {
                                    role.addPermission(new Permission(p[0], p[1], p[2]));
                                }
                            }
                        }
                        system.getRoleManager().add(role);
                    }
                }
                case "[ASSIGNMENTS]" -> assignmentRows.add(parts);
                default -> {
                }
            }
        }

        for (String[] parts : assignmentRows) {
            if (parts.length < 7) continue;
            String type = parts[0];
            Optional<User> userOpt = system.getUserManager().findByUsername(parts[1]);
            Optional<Role> roleOpt = system.getRoleManager().findByName(parts[2]);
            if (userOpt.isEmpty() || roleOpt.isEmpty()) continue;

            AssignmentMetadata md = new AssignmentMetadata(parts[3], parts[4], parts[5]);
            try {
                if ("TEMPORARY".equals(type)) {
                    TemporaryAssignment t = new TemporaryAssignment(userOpt.get(), roleOpt.get(), md, parts[6], false);
                    system.getAssignmentManager().add(t);
                } else {
                    PermanentAssignment p = new PermanentAssignment(userOpt.get(), roleOpt.get(), md);
                    system.getAssignmentManager().add(p);
                    if ("true".equalsIgnoreCase(parts[6])) {
                        p.revoke();
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static Map<String, List<Permission>> groupPermissionsByResource(Set<Permission> permissions) {
        return permissions.stream()
                .collect(Collectors.groupingBy(Permission::resource, LinkedHashMap::new, Collectors.toList()));
    }
}
