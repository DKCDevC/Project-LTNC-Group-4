package network.command;

import java.util.HashMap;
import java.util.Map;

public class CommandRouter {
    private static CommandRouter instance;
    private final Map<String, Command> commands = new HashMap<>();

    private CommandRouter() {}

    public static synchronized CommandRouter getInstance() {
        if (instance == null) {
            instance = new CommandRouter();
        }
        return instance;
    }

    public void registerCommand(String commandName, Command command) {
        commands.put(commandName, command);
    }

    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
}
