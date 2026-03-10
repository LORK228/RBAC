public class CommandRegistry {

    public static void registerAll(CommandParser parser) {
        UserCommands.register(parser);
        RoleCommands.register(parser);
        AssignmentCommands.register(parser);
        PermissionCommands.register(parser);
        ServiceCommands.register(parser);
    }
}
