package usecase.admin;

import domain.repository.AdminRepository;
import dao.AdminJDBCRepository;
import models.User;
import java.util.List;

public class UserManagementUseCase {
    private static UserManagementUseCase instance;
    private final AdminRepository repository;

    private UserManagementUseCase(AdminRepository repository) {
        this.repository = repository;
    }

    public static synchronized UserManagementUseCase getInstance() {
        if (instance == null) {
            instance = new UserManagementUseCase(AdminJDBCRepository.getInstance());
        }
        return instance;
    }

    public List<User> getUsers() {
        return repository.getAllUsers();
    }

    public boolean lockUser(String username) {
        return repository.updateUserStatus(username, true);
    }

    public boolean unlockUser(String username) {
        return repository.updateUserStatus(username, false);
    }

    public boolean verifySeller(String username) {
        return repository.verifySeller(username, true);
    }

    public boolean removeUser(String username) {
        return repository.deleteUser(username);
    }
}
