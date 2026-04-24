package network.commands;

import com.google.gson.JsonObject;
import network.ClientHandler;

/**
 * Command Pattern interface for handling client requests on the server.
 */
public interface ServerCommand {
    void execute(ClientHandler handler, JsonObject req);
}
