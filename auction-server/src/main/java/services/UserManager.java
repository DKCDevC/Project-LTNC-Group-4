package services;

import dao.UserDAO;
import models.User;

public class UserManager {
    private static UserManager instance;
    private UserDAO userDAO;

    private UserManager() {
        userDAO = new UserDAO();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

<<<<<<< HEAD
    public boolean register(User newUser) {
        return userDAO.registerUser(newUser);
    }

=======
    // Hàm Login
>>>>>>> 6dd5a76 (change login)
    public User login(String username, String password) {
        return userDAO.loginUser(username, password);
    }

<<<<<<< HEAD
=======
    // Hàm Register
>>>>>>> 6dd5a76 (change login)
    public boolean registerUser(String username, String password, String role) {
        return userDAO.insertUser(username, password, role);
    }
}