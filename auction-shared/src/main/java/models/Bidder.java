package models;

public class Bidder extends User {
    public Bidder(String username, String password, String email) {
        super(username, password, email);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}
