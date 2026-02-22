public record Permission(String name, String resource, String description) {
    public Permission {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (name.isEmpty()) throw new IllegalArgumentException("name must not be empty");
        if (name.indexOf(' ') >= 0) throw new IllegalArgumentException("name must not contain spaces");
        name = name.toUpperCase();

        if (resource == null) throw new IllegalArgumentException("resource must not be null");
        if (resource.isEmpty()) throw new IllegalArgumentException("resource must not be empty");
        resource = resource.toLowerCase();

        if (description == null) throw new IllegalArgumentException("description must not be null");
        if (description.isEmpty()) throw new IllegalArgumentException("description must not be empty");
    }

    public String format() {
        return name + " on " + resource + ": " + description;
    }

    public boolean matches(String namePattern, String resourcePattern)
    {
        if (namePattern != null)
        {
            String np = namePattern.trim().toUpperCase();
            if (np.isEmpty())
            {/* пустой шаблон — считаем как незаданный*/}
            else if (!name.contains(np))
                return false;

        }
        if (resourcePattern != null)
        {
            String rp = resourcePattern.trim().toLowerCase();
            if (rp.isEmpty())
            {/* пустой шаблон — считаем как незаданный*/}
            else if (!resource.contains(rp))
                return false;
        }
        return true;
    }
}
