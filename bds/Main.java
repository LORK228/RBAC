import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (RBACSystem system = new RBACSystem()) {
            system.initialize();

            CommandParser parser = new CommandParser();
            CommandRegistry.registerAll(parser);

            ConsoleUtils.printSection("RBAC Console");
            parser.printHelp();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine();
                if (line == null) {
                    break;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                try {
                    parser.parseAndExecute(trimmed, scanner, system);
                } catch (ExitApplicationException ex) {
                    ConsoleUtils.printSuccess("Bye");
                    return;
                } catch (Exception ex) {
                    ConsoleUtils.printError(ex.getMessage());
                }
            }
        }
    }
}
