package network.command;

import com.google.gson.JsonObject;
import network.ClientHandler;
import java.io.PrintWriter;

public interface Command {
    void execute(JsonObject requestData, PrintWriter out, ClientHandler handler);
}
