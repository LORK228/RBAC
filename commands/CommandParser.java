import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, String> commandDescriptions = new LinkedHashMap<>();
    private String lastArgs = "";

    public void registerCommand(String name, String description, Command command) {
        ValidationUtils.requireNonEmpty(name, "command name");
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        String key = name.trim().toLowerCase();
        String desc = description == null ? "" : description.trim();

        commands.put(key, command);
        commandDescriptions.put(key, desc);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (commandName == null || commandName.trim().isEmpty()) {
            ConsoleUtils.printError("Command name is empty");
            return;
        }
        if (scanner == null) {
            throw new IllegalArgumentException("scanner must not be null");
        }
        if (system == null) {
            throw new IllegalArgumentException("system must not be null");
        }

        String key = commandName.trim().toLowerCase();
        Command command = commands.get(key);
        if (command == null) {
            ConsoleUtils.printError("Unknown command: " + commandName);
            ConsoleUtils.printSuccess("Type 'help' to see available commands");
            return;
        }
        command.execute(scanner, system);
    }

    public void printHelp() {
        if (commands.isEmpty()) {
            System.out.println(FormatUtils.formatBox("No commands registered"));
            return;
        }

        List<String[]> rows = commands.keySet().stream()
                .map(name -> new String[]{
                        name,
                        FormatUtils.truncate(commandDescriptions.getOrDefault(name, ""), 80)
                })
                .toList();

        System.out.println(FormatUtils.formatHeader("Command Help"));
        System.out.println(FormatUtils.formatTable(new String[]{"Command", "Description"}, rows));
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        String trimmed = input.trim();
        int firstSpace = trimmed.indexOf(' ');
        String commandName;
        if (firstSpace < 0) {
            commandName = trimmed;
            lastArgs = "";
        } else {
            commandName = trimmed.substring(0, firstSpace).trim();
            lastArgs = trimmed.substring(firstSpace + 1).trim();
        }
        executeCommand(commandName, scanner, system);
    }

    public String getLastArgs() {
        return lastArgs;
    }
}
