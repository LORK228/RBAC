public record Permission(String name, String resource, String description) {
    public Permission {
        ValidationUtils.requireNonEmpty(name, "name");
        ValidationUtils.requireNonEmpty(resource, "resource");
        ValidationUtils.requireNonEmpty(description, "description");

        String normalizedName = ValidationUtils.normalizeString(name);
        if (normalizedName.indexOf(' ') >= 0) throw new IllegalArgumentException("name must not contain spaces");
        name = normalizedName.toUpperCase();

        resource = ValidationUtils.normalizeString(resource);
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
