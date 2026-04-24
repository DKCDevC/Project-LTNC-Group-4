package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import utils.GsonConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Singleton Manager for a single persistent TCP connection.
 * Handles background listening and message dispatching.
 */
public class SocketManager {
    private static volatile SocketManager instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Gson gson = GsonConfig.createGson();
    private volatile boolean running = false;

    // Listeners for different server commands
    private final Map<String, Consumer<JsonObject>> listeners = new ConcurrentHashMap<>();

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect(String host, int port) throws Exception {
        if (socket != null && !socket.isClosed()) return;
        
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        startListenerThread();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void send(Object obj) {
        if (out != null) {
            out.println(gson.toJson(obj));
        }
    }

    public void addListener(String command, Consumer<JsonObject> callback) {
        listeners.put(command, callback);
    }

    public void removeListener(String command) {
        listeners.remove(command);
    }

    public void clearListeners() {
        listeners.clear();
    }

    private void startListenerThread() {
        if (running) return;
        running = true;
        
        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    try {
                        JsonObject resp = gson.fromJson(line, JsonObject.class);
                        String cmd = resp.has("command") ? resp.get("command").getAsString() : "";
                        
                        Consumer<JsonObject> callback = listeners.get(cmd);
                        if (callback != null) {
                            Platform.runLater(() -> callback.accept(resp));
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing server message: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Connection lost: " + e.getMessage());
            } finally {
                close();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (Exception ignored) {}
    }
}
