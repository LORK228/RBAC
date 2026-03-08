import java.util.*;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role>
{
    private Map<String, Role> rolesById = new HashMap<>();

    private Map<String, Role> rolesByName = new HashMap<>();

    private AssignmentManager assignmentManager;
    private AuditLog auditLog;

    public RoleManager()
    {
        this(null, null);
    }

    public RoleManager(AuditLog auditLog)
    {
        this(null, auditLog);
    }

    public RoleManager(AssignmentManager assignmentManager)
    {
        this(assignmentManager, null);
    }

    public RoleManager(AssignmentManager assignmentManager, AuditLog auditLog)
    {
        this.assignmentManager = assignmentManager;
        this.auditLog = auditLog;
    }

    public void setAssignmentManager(AssignmentManager assignmentManager)
    {
        this.assignmentManager = assignmentManager;
    }

    public void setAuditLog(AuditLog auditLog)
    {
        this.auditLog = auditLog;
    }

    @Override
    public void add(Role item)
    {
        if (item == null)
            throw new IllegalArgumentException("Role cannot be null");

        if (rolesById.containsKey(item.getId()))
            throw new IllegalArgumentException("Role with ID '" + item.getId() + "' already exists");

        if (rolesByName.containsKey(item.getName()))
            throw new IllegalArgumentException("Role with name '" + item.getName() + "' already exists");

        rolesById.put(item.getId(), item);
        rolesByName.put(item.getName(), item);
        if (auditLog != null) {
            auditLog.log("CREATE_ROLE", "system", item.getName(),
                    "Role created with id " + item.getId());
        }
    }

    @Override
    public boolean remove(Role item)
    {
        if (item == null)
            return false;

        if (assignmentManager != null)
        {
            List<RoleAssignment> assignmentsForRole = assignmentManager.findByRole(item);
            boolean hasActive = assignmentsForRole != null && assignmentsForRole.stream()
                    .anyMatch(RoleAssignment::isActive);

            if (hasActive)
            {
                throw new IllegalArgumentException(
                        "Cannot remove role '" + item.getName() + "' because it is assigned to users");
            }
        }

        boolean removed = rolesById.remove(item.getId()) != null;
        if (removed)
        {
            rolesByName.remove(item.getName());
            if (auditLog != null) {
                auditLog.log("DELETE_ROLE", "system", item.getName(),
                        "Role deleted with id " + item.getId());
            }
        }

        return removed;
    }


    @Override
    public Optional<Role> findById(String id)
    {
        if (id == null || id.isEmpty())
            return Optional.empty();

        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll()
    {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count()
    {
        return rolesById.size();
    }

    @Override
    public void clear()
    {
        rolesById.clear();
        rolesByName.clear();
    }


    public Optional<Role> findByName(String name)
    {
        if (name == null || name.isEmpty())
            return Optional.empty();

        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter)
    {
        if (filter == null)
            return new ArrayList<>();

        return rolesById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter)
    {
        if (filter == null && sorter == null)
            return findAll();

        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name)
    {
        if (name == null || name.isEmpty())
            return false;

        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission)
    {
        if (roleName == null || roleName.isEmpty())
            throw new IllegalArgumentException("Role name cannot be null or empty");

        if (permission == null)
            throw new IllegalArgumentException("Permission cannot be null");

        Role role = rolesByName.get(roleName);
        if (role == null)
            throw new IllegalArgumentException("Role with name '" + roleName + "' not found");

        role.addPermission(permission);
    }


    public void removePermissionFromRole(String roleName, Permission permission)
    {
        if (roleName == null || roleName.isEmpty())
            throw new IllegalArgumentException("Role name cannot be null or empty");

        if (permission == null)
            throw new IllegalArgumentException("Permission cannot be null");

        Role role = rolesByName.get(roleName);
        if (role == null)
            throw new IllegalArgumentException("Role with name '" + roleName + "' not found");

        role.removePermission(permission);
    }


    public List<Role> findRolesWithPermission(String permissionName, String resource)
    {
        if (permissionName == null || permissionName.isEmpty())
            throw new IllegalArgumentException("Permission name cannot be null or empty");

        if (resource == null || resource.isEmpty())
            throw new IllegalArgumentException("Resource cannot be null or empty");

        return rolesById.values().stream()
                .filter(role -> role.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }

    @Override
    public String toString()
    {
        return "RoleManager{" +
                "rolesById=" + rolesById.size() + " roles" +
                ", rolesByName=" + rolesByName.size() + " roles indexed" +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RoleManager)) return false;

        RoleManager that = (RoleManager) o;
        return Objects.equals(rolesById, that.rolesById) &&
                Objects.equals(rolesByName, that.rolesByName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(rolesById, rolesByName);
    }
}
