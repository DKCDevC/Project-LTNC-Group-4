package services;

import dao.UserDAO;
import models.User;
import network.ClientHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user authentication and enforces single-session-per-user policy.
 *
 * When a user logs in from a second device, the first session is kicked
 * atomically using ConcurrentHashMap.compute() to prevent race conditions.
 */
public class UserManager {

    private static UserManager instance;
    private final UserDAO userDAO;

    /**
     * Active sessions: username -> ClientHandler that owns the session.
     * Using ConcurrentHashMap ensures thread-safe reads, but the critical
     * login logic additionally uses compute() for atomicity.
     */
    private final ConcurrentHashMap<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    private UserManager() {
        userDAO = new UserDAO();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    /** Validates credentials against the DB. Returns User if valid, null otherwise. */
    public User authenticate(String username, String password) {
        return userDAO.loginUser(username, password);
    }

    /** @deprecated Use {@link #authenticate(String, String)} instead. */
    @Deprecated
    public User login(String username, String password) {
        return authenticate(username, password);
    }

    public boolean registerUser(String username, String password, String role) {
        return userDAO.insertUser(username, password, role);
    }

    // ------------------------------------------------------------------
    // Session management
    // ------------------------------------------------------------------

    /**
     * Registers a new session for {@code username}.
     * If another session already exists for the same user it is kicked
     * and replaced atomically.
     *
     * @param username the authenticated user
     * @param handler  the ClientHandler that owns this session
     */
    public void registerSession(String username, ClientHandler handler) {
        activeSessions.compute(username, (k, oldHandler) -> {
            if (oldHandler != null && oldHandler != handler) {
                // Kick the old session before replacing
                oldHandler.sendKick("Tài khoản của bạn đã được đăng nhập ở thiết bị khác!");
            }
            return handler;
        });
        System.out.println(">>> Session registered for: " + username
                + " | Active sessions: " + activeSessions.size());
    }

    /**
     * Removes the session for {@code username} if it is still owned by {@code handler}.
     * Called when a client disconnects (IOException / ping timeout).
     */
    public void removeSession(String username, ClientHandler handler) {
        activeSessions.remove(username, handler); // only removes if value matches
        System.out.println(">>> Session removed for: " + username
                + " | Active sessions: " + activeSessions.size());
    }

    public ClientHandler getSession(String username) {
        return activeSessions.get(username);
    }

    /** Returns true if a session is active for the given username. */
    public boolean isSessionActive(String username) {
        return activeSessions.containsKey(username);
    }

    /** Number of currently connected users. */
    public int activeSessionCount() {
        return activeSessions.size();
    }
}
