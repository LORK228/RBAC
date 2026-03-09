import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.roleManager.setAssignmentManager(this.assignmentManager);
        this.currentUser = "system";
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        userManager.clear();
        roleManager.clear();
        assignmentManager.clear();

        Permission readUsers = new Permission("READ", "users", "Read users");
        Permission writeUsers = new Permission("WRITE", "users", "Create and update users");
        Permission deleteUsers = new Permission("DELETE", "users", "Delete users");

        Permission readRoles = new Permission("READ", "roles", "Read roles");
        Permission writeRoles = new Permission("WRITE", "roles", "Create and update roles");
        Permission deleteRoles = new Permission("DELETE", "roles", "Delete roles");

        Permission readAssignments = new Permission("READ", "assignments", "Read assignments");
        Permission writeAssignments = new Permission("WRITE", "assignments", "Create assignments");
        Permission deleteAssignments = new Permission("DELETE", "assignments", "Revoke assignments");

        Role adminRole = new Role("Admin", "Full access");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readRoles);
        adminRole.addPermission(writeRoles);
        adminRole.addPermission(deleteRoles);
        adminRole.addPermission(readAssignments);
        adminRole.addPermission(writeAssignments);
        adminRole.addPermission(deleteAssignments);

        Role managerRole = new Role("Manager", "Manage users and assignments");
        managerRole.addPermission(readUsers);
        managerRole.addPermission(writeUsers);
        managerRole.addPermission(readAssignments);
        managerRole.addPermission(writeAssignments);

        Role viewerRole = new Role("Viewer", "Read only");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readRoles);
        viewerRole.addPermission(readAssignments);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User admin = User.validate("admin", "System Administrator", "admin@example.com");
        userManager.add(admin);

        AssignmentMetadata metadata = AssignmentMetadata.now(currentUser, "Initial bootstrap");
        PermanentAssignment assignment = new PermanentAssignment(admin, adminRole, metadata);
        assignmentManager.add(assignment);
    }

    public String generateStatistics() {
        int users = userManager.count();
        int roles = roleManager.count();
        int assignmentsTotal = assignmentManager.count();
        int assignmentsActive = assignmentManager.getActiveAssignments().size();
        int assignmentsExpired = assignmentManager.getExpiredAssignments().size();

        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("RBAC Statistics")).append("\n\n");

        List<String[]> rows = List.of(
                new String[]{"Users", String.format("%d", users)},
                new String[]{"Roles", String.format("%d", roles)},
                new String[]{"Assignments (total)", String.format("%d", assignmentsTotal)},
                new String[]{"Assignments (active)", String.format("%d", assignmentsActive)},
                new String[]{"Assignments (expired)", String.format("%d", assignmentsExpired)}
        );
        sb.append(FormatUtils.formatTable(new String[]{"Metric", "Value"}, rows));

        List<Role> popularRoles = roleManager.findAll().stream()
                .sorted(Comparator.comparingLong((Role r) -> assignmentManager.findByRole(r).stream()
                        .filter(RoleAssignment::isActive)
                        .map(a -> a.user().username())
                        .distinct()
                        .count()).reversed())
                .limit(3)
                .collect(Collectors.toList());

        if (!popularRoles.isEmpty()) {
            sb.append("\n\n").append(FormatUtils.formatHeader("Top Roles")).append("\n\n");
            List<String[]> topRows = popularRoles.stream()
                    .map(r -> new String[]{
                            r.getName(),
                            String.format("%d", assignmentManager.findByRole(r).stream()
                                    .filter(RoleAssignment::isActive)
                                    .map(a -> a.user().username())
                                    .distinct()
                                    .count())
                    })
                    .collect(Collectors.toList());
            sb.append(FormatUtils.formatTable(new String[]{"Role", "Active users"}, topRows));
        }

        return sb.toString();
    }
}
