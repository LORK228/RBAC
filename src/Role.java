import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.text.MessageFormat;

public class Role
{
    private String id;
    private String name;
    private String description;
    private Set<Permission> permissions;

    public Role(String name, String description)
    {
        this.id = "role_" + UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    public Role()
    {
        this("", "");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void addPermission(Permission permission)
    {
        permissions.add(permission);
    }

    public void removePermission(Permission permission)
    {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission)
    {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource)
    {
        return permissions
                .stream()
                .anyMatch(x -> x.name().equals(permissionName) && x.resource().equals(resource));
    }

    public Set<Permission> getPermissions()
    {
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Role role = (Role) obj;
        return id.equals(role.id);
    }

    @Override
    public String toString()
    {
        return MessageFormat.format("Role: {0} [ID: {1}]", name, id);
    }

    public String format()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageFormat.format("Role: {0} [ID: {1}]\n", name, id));
        sb.append(MessageFormat.format("Description: {0}\n", description));
        sb.append(MessageFormat.format("Permissions ({0}):\n", permissions.size()));

        for (Permission p : permissions) {
            sb.append(MessageFormat.format(" - {0}\n", p.format()));
        }

        return sb.toString();
    }
}
