import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignmentsById = new ConcurrentHashMap<>();
    private final Object assignmentsLock = new Object();

    private final UserManager userManager;
    private final RoleManager roleManager;
    private AuditLog auditLog;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this(userManager, roleManager, null);
    }

    public AssignmentManager(UserManager userManager, RoleManager roleManager, AuditLog auditLog) {
        if (userManager == null) throw new IllegalArgumentException("UserManager cannot be null");
        if (roleManager == null) throw new IllegalArgumentException("RoleManager cannot be null");
        this.userManager = userManager;
        this.roleManager = roleManager;
        this.auditLog = auditLog;
    }

    public void setAuditLog(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException("RoleAssignment cannot be null");
        ValidationUtils.requireNonEmpty(item.assignmentId(), "Assignment ID");

        synchronized (assignmentsLock) {
            if (assignmentsById.containsKey(item.assignmentId())) {
                throw new IllegalArgumentException("Assignment with ID '" + item.assignmentId() + "' already exists");
            }

            if (!userManager.exists(item.user().username())) {
                throw new IllegalArgumentException("User '" + item.user().username() + "' does not exist");
            }

            if (roleManager.findById(item.role().getId()).isEmpty()) {
                throw new IllegalArgumentException("Role with ID '" + item.role().getId() + "' does not exist");
            }

            boolean duplicateActive = assignmentsById.values().stream()
                    .anyMatch(a -> a.user().username().equals(item.user().username())
                            && a.role().getId().equals(item.role().getId())
                            && a.isActive());

            if (duplicateActive) {
                throw new IllegalArgumentException(String.format(
                        "User '%s' already has an active assignment for role '%s'",
                        item.user().username(), item.role().getName()));
            }

            assignmentsById.put(item.assignmentId(), item);
        }

        if (auditLog != null) {
            auditLog.log("ASSIGN_ROLE", item.metadata().assignedBy(), item.assignmentId(),
                    String.format("Role '%s' assigned to user '%s'",
                            item.role().getName(), item.user().username()));
        }
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
        synchronized (assignmentsLock) {
            assignmentsById.clear();
        }
    }

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

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        if (filter == null) return Collections.emptyList();
        return assignmentsById.values().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        return assignmentsById.values().stream()
                .filter(a -> filter == null || filter.test(a))
                .sorted(sorter != null ? sorter : Comparator.comparing(RoleAssignment::assignmentId))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignmentsById.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignmentsById.values().stream()
                .filter(a -> a instanceof TemporaryAssignment)
                .map(a -> (TemporaryAssignment) a)
                .filter(TemporaryAssignment::isExpired)
                .collect(Collectors.toList());
    }

    public int deactivateExpiredTemporaryAssignments() {
        List<TemporaryAssignment> expired = assignmentsById.values().stream()
                .filter(a -> a instanceof TemporaryAssignment)
                .map(a -> (TemporaryAssignment) a)
                .filter(TemporaryAssignment::isExpired)
                .filter(a -> !a.isMarkedInactive())
                .collect(Collectors.toList());

        for (TemporaryAssignment assignment : expired) {
            assignment.markInactive();
        }
        return expired.size();
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) return false;
        return assignmentsById.values().stream()
                .anyMatch(a -> a.user().username().equals(user.username())
                        && a.role().getId().equals(role.getId())
                        && a.isActive());
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

        RoleAssignment assignment;
        synchronized (assignmentsLock) {
            assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment with ID '" + assignmentId + "' not found");
            }

            if (assignment instanceof PermanentAssignment) {
                ((PermanentAssignment) assignment).revoke();
            } else {
                assignmentsById.remove(assignmentId);
            }
        }

        if (auditLog != null) {
            auditLog.log("REVOKE_ROLE", "system", assignmentId,
                    String.format("Role '%s' revoked from user '%s'",
                            assignment.role().getName(), assignment.user().username()));
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        ValidationUtils.requireNonEmpty(assignmentId, "Assignment ID");
        ValidationUtils.requireNonEmpty(newExpirationDate, "New expiration date");
        String normalizedDate = newExpirationDate.trim();
        if (!ValidationUtils.isValidDate(normalizedDate)) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss");
        }
        String toExtend = normalizedDate.contains("T")
                ? normalizedDate
                : DateUtils.addDays(normalizedDate, 0) + "T23:59:59";

        synchronized (assignmentsLock) {
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment with ID '" + assignmentId + "' not found");
            }

            if (!(assignment instanceof TemporaryAssignment)) {
                throw new IllegalArgumentException("Assignment with ID '" + assignmentId + "' is not temporary");
            }

            ((TemporaryAssignment) assignment).extend(toExtend);
        }
    }
}
