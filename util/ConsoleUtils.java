import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {
    private static final String BORDER = "============================================================";

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + ": ");
            String input = scanner.nextLine();
            if (input == null) {
                input = "";
            }
            String trimmed = input.trim();

            if (!required) {
                return trimmed;
            }
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
            printError("Input is required. Try again.");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(String.format("%s (%d-%d): ", message, min, max));
            String raw = scanner.nextLine();
            try {
                int value = Integer.parseInt(raw.trim());
                if (value < min || value > max) {
                    printError(String.format("Value must be between %d and %d.", min, max));
                    continue;
                }
                return value;
            } catch (Exception ex) {
                printError("Invalid number. Try again.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (yes/no): ");
            String input = scanner.nextLine();
            if (input == null) {
                input = "";
            }
            String normalized = input.trim().toLowerCase();
            if (normalized.equals("yes") || normalized.equals("y")) {
                return true;
            }
            if (normalized.equals("no") || normalized.equals("n")) {
                return false;
            }
            printError("Please answer yes or no.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list must not be empty");
        }

        printSection(message);
        for (int i = 0; i < options.size(); i++) {
            System.out.println(String.format("  %d) %s", i + 1, options.get(i)));
        }

        int choice = promptInt(scanner, "Choose option number", 1, options.size());
        return options.get(choice - 1);
    }

    public static void printSection(String title) {
        System.out.println();
        System.out.println(BORDER);
        System.out.println("  " + title);
        System.out.println(BORDER);
    }

    public static void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }
}
