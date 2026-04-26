package dao;

import models.User;

public interface UserRepository {
    User loginUser(String identifier, String password);
    boolean insertUser(String username, String password, String email, String role);
}
