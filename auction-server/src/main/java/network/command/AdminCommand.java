package network.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import usecase.admin.UserManagementUseCase;
import utils.GsonConfig;
import network.ClientHandler;
import java.io.PrintWriter;

public class AdminCommand implements Command {
    private final Gson gson = GsonConfig.createGson();

    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // RBAC Check: Đảm bảo chỉ Admin mới được dùng các lệnh này
        // Lưu ý: Trong thực tế nên kiểm tra role từ database hoặc token
        String subCommand = requestData.has("subCommand") ? requestData.get("subCommand").getAsString() : "";
        
        switch (subCommand) {
            case "GET_USERS":
                out.println(gson.toJson(UserManagementUseCase.getInstance().getUsers()));
                break;
                
            case "LOCK_USER":
                String userToLock = requestData.get("targetUser").getAsString();
                boolean lockSuccess = UserManagementUseCase.getInstance().lockUser(userToLock);
                out.println(lockSuccess ? "SUCCESS" : "FAILED");
                break;
                
            case "UNLOCK_USER":
                String userToUnlock = requestData.get("targetUser").getAsString();
                boolean unlockSuccess = UserManagementUseCase.getInstance().unlockUser(userToUnlock);
                out.println(unlockSuccess ? "SUCCESS" : "FAILED");
                break;

            case "VERIFY_SELLER":
                String sellerToVerify = requestData.get("targetUser").getAsString();
                boolean verifySuccess = UserManagementUseCase.getInstance().verifySeller(sellerToVerify);
                out.println(verifySuccess ? "SUCCESS" : "FAILED");
                break;

            default:
                out.println("ERROR: Unknown Admin sub-command");
                break;
        }
    }
}
