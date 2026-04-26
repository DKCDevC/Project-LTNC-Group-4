package network.command;

import com.google.gson.JsonObject;
import models.User;
import services.UserManager;
import network.ClientHandler;
import java.io.PrintWriter;

public class LoginCommand implements Command {
    @Override
    public void execute(JsonObject loginData, PrintWriter out, ClientHandler handler) {
        String identifier = loginData.has("username") ? loginData.get("username").getAsString() : 
                            (loginData.has("email") ? loginData.get("email").getAsString() : "");
        String password = loginData.has("password") ? loginData.get("password").getAsString() : "";

        User loggedInUser = UserManager.getInstance().login(identifier, password);

        if (loggedInUser != null) {
            if (loggedInUser.isLocked()) {
                out.println("{\"status\":\"FAILED\", \"message\":\"Tài khoản của bạn đã bị khóa bởi Admin!\"}");
                System.out.println(">>> Từ chối đăng nhập: User [" + loggedInUser.getUsername() + "] đang bị khóa.");
                return;
            }
            handler.setCurrentUsername(loggedInUser.getUsername()); // Ghi nhận phiên đăng nhập
            out.println("{\"status\":\"SUCCESS\", \"role\":\"" + loggedInUser.getRole() + "\"}");
            System.out.println(">>> User [" + loggedInUser.getUsername() + "] đã đăng nhập thành công!");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Sai tai khoản hoặc mật khẩu\"}");
        }
    }
}
