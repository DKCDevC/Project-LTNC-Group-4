package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class AuctionClient {
    private static AuctionClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private AuctionClient() {
        connect();
    }

    public static synchronized AuctionClient getInstance() {
        if (instance == null) {
            instance = new AuctionClient();
        }
        return instance;
    }

    private void connect() {
        try {
            socket = new Socket("127.0.0.1", 9999); // Port updated to 9999 for MainServer
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            // Background thread to listen for broadcast messages
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("[SERVER BROADCAST]: " + msg);
                    }
                } catch (Exception e) {
                    connected = false;
                }
            }).start();
            
        } catch (Exception e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }

    public void sendRequest(String json, Consumer<String> callback) {
        if (!connected) {
            connect();
        }
        
        if (connected) {
            new Thread(() -> {
                try {
                    out.println(json);
                    // This is a simple synchronous-over-asynchronous approach for a basic system
                    // In a production system, we'd use Request IDs to match responses
                    String response = in.readLine(); 
                    if (callback != null) {
                        callback.accept(response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    // For legacy console app support if needed
    public static void main(String[] args) {
        System.out.println("AuctionClient service mode enabled.");
    }
}