import java.util.*;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignmentsById = new HashMap<>();

    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        if (userManager == null) throw new IllegalArgumentException("UserManager cannot be null");
        if (roleManager == null) throw new IllegalArgumentException("RoleManager cannot be null");
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException("RoleAssignment cannot be null");
        ValidationUtils.requireNonEmpty(item.assignmentId(), "Assignment ID");

        if (assignmentsById.containsKey(item.assignmentId()))
            throw new IllegalArgumentException("Assignment with ID '" + item.assignmentId() + "' already exists");

        // Проверяем существование пользователя и роли в соответствующих менеджерах
        if (!userManager.exists(item.user().username()))
            throw new IllegalArgumentException("User '" + item.user().username() + "' does not exist");

        if (!roleManager.findById(item.role().getId()).isPresent())
            throw new IllegalArgumentException("Role with ID '" + item.role().getId() + "' does not exist");

        // Не допускаем, чтобы одна роль была назначена пользователю дважды одновременно (активно)
        boolean duplicateActive = assignmentsById.values().stream()
                .anyMatch(a ->
                        a.user().username().equals(item.user().username())
                                && a.role().getId().equals(item.role().getId())
                                && a.isActive()
                );

        if (duplicateActive)
            throw new IllegalArgumentException(String.format(
                    "User '%s' already has an active assignment for role '%s'",
                    item.user().username(), item.role().getName()));

        assignmentsById.put(item.assignmentId(), item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) return false;
        return assignmentsById.remove(item.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(assignmentsById.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignmentsById.values());
    }

    @Override
    public int count() {
        return assignmentsById.size();
    }

    @Override
    public void clear() {
        assignmentsById.clear();
    }

    // --- Additional required methods ---

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) return Collections.emptyList();
        return assignmentsById.values().stream()
                .filter(a -> a.user().username().equals(user.username()))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) return Collections.emptyList();
        return assignmentsById.values().stream()
                .filter(a -> a.role().getId().equals(role.getId()))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) return Collections.emptyList();
        return assignmentsById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        return assignmentsById.values().stream()
                .filter(a -> filter == null || filter.test(a))
                // безопасно применяем сортировку только если sorter != null
                .sorted(sorter != null ? sorter : Comparator.comparing(RoleAssignment::assignmentId))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignmentsById.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        // используем TemporaryAssignment::isExpired для явности
        return assignmentsById.values().stream()
                .filter(a -> a instanceof TemporaryAssignment)
                .map(a -> (TemporaryAssignment) a)
                .filter(TemporaryAssignment::isExpired)
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) return false;
        return assignmentsById.values().stream()
                .anyMatch(a ->
                        a.user().username().equals(user.username())
                                && a.role().getId().equals(role.getId())
                                && a.isActive()
                );
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) return false;
        String normalizedName = ValidationUtils.normalizeString(permissionName);
        String normalizedResource = ValidationUtils.normalizeString(resource);
        if (normalizedName == null || normalizedResource == null) return false;
        if (normalizedName.isEmpty() || normalizedResource.isEmpty()) return false;

        return assignmentsById.values().stream()
                .filter(a -> a.user().username().equals(user.username()))
                .filter(RoleAssignment::isActive)
                .anyMatch(a -> a.role().hasPermission(normalizedName, normalizedResource));
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) return Collections.emptySet();
        return assignmentsById.values().stream()
                .filter(a -> a.user().username().equals(user.username()))
                .filter(RoleAssignment::isActive)
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        ValidationUtils.requireNonEmpty(assignmentId, "Assignment ID");

        RoleAssignment assignment = assignmentsById.get(assignmentId);
        if (assignment == null)
            throw new IllegalArgumentException("Assignment with ID '" + assignmentId + "' not found");

        if (assignment instanceof PermanentAssignment) {
            ((PermanentAssignment) assignment).revoke();
        } else {
            assignmentsById.remove(assignmentId);
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        ValidationUtils.requireNonEmpty(assignmentId, "Assignment ID");
        ValidationUtils.requireNonEmpty(newExpirationDate, "New expiration date");
        String normalizedDate = newExpirationDate.trim();
        if (!ValidationUtils.isValidDate(normalizedDate)) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss");
        }

        RoleAssignment assignment = assignmentsById.get(assignmentId);
        if (assignment == null)
            throw new IllegalArgumentException("Assignment with ID '" + assignmentId + "' not found");

        if (!(assignment instanceof TemporaryAssignment))
            throw new IllegalArgumentException("Assignment with ID '" + assignmentId + "' is not temporary");

        ((TemporaryAssignment) assignment).extend(normalizedDate);
    }
}
