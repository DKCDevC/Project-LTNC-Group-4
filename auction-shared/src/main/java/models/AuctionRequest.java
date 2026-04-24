package models;

import java.util.Objects;

public class AuctionRequest {
    private String command;
    private Object data;

    public AuctionRequest() {}

    public AuctionRequest(String command, Object data) {
        this.command = command;
        this.data = data;
    }

    public String getCommand() { return command; }
    public Object getData() { return data; }
}
