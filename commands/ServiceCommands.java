public class ServiceCommands {
    public static void register(CommandParser parser) {
        parser.registerCommand("help", "Show help", (scanner, system) -> parser.printHelp());

        parser.registerCommand("stats", "Show system statistics", (scanner, system) ->
                System.out.println(system.generateStatistics()));

        parser.registerCommand("clear", "Clear screen", (scanner, system) -> {
            for (int i = 0; i < 40; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit program", (scanner, system) -> {
            if (ConsoleUtils.promptYesNo(scanner, "Exit application?")) {
                throw new ExitApplicationException();
            }
        });

        parser.registerCommand("save", "Save data to file", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Filename", true);
            CommandSupport.saveSystem(system, filename);
            ConsoleUtils.printSuccess("Saved to " + filename);
        });

        parser.registerCommand("save-async", "Save data to file in background", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Filename", true);
            system.saveSystemAsync(filename);
            ConsoleUtils.printSuccess("Background save started for " + filename);
        });

        parser.registerCommand("report-users-async", "Generate user report in background", (scanner, system) -> {
            system.generateUsersReportAsync();
            ConsoleUtils.printSuccess("Background user report started");
        });

        parser.registerCommand("load", "Load data from file", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Filename", true);
            CommandSupport.loadSystem(system, filename);
            ConsoleUtils.printSuccess("Loaded from " + filename);
        });
    }
}
