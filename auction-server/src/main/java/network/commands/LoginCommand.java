package network.commands;

import com.google.gson.JsonObject;
import network.ClientHandler;
import services.UserManager;
import models.User;

public class LoginCommand implements ServerCommand {
    @Override
    public void execute(ClientHandler handler, JsonObject req) {
        String username = req.has("username") ? req.get("username").getAsString() : "";
        String password = req.has("password") ? req.get("password").getAsString() : "";

        User user = UserManager.getInstance().authenticate(username, password);
        if (user != null) {
            handler.setSessionUsername(username);
            UserManager.getInstance().registerSession(username, handler);

            handler.getOut().println("{\"command\":\"LOGIN_RESPONSE\",\"status\":\"SUCCESS\",\"role\":\"" + user.getRole() + "\"}");
            System.out.println(">>> [Command] LOGIN OK: " + username);
        } else {
            handler.getOut().println("{\"command\":\"LOGIN_RESPONSE\",\"status\":\"FAILED\",\"message\":\"Invalid credentials\"}");
        }
    }
}
