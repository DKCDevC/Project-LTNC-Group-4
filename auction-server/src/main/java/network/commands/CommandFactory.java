package network.commands;

import java.util.HashMap;
import java.util.Map;

public class CommandFactory {
    private static final Map<String, ServerCommand> commands = new HashMap<>();

    static {
        commands.put("LOGIN", new LoginCommand());
        commands.put("BID", new BidCommand());
        commands.put("REGISTER_AUTOBID", new AutoBidCommand());
        // More commands can be registered here
    }

    public static ServerCommand getCommand(String name) {
        return commands.get(name.toUpperCase());
    }
}
