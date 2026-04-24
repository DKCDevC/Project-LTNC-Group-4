package models;

public class Seller extends User {
    public Seller(String username, String password, String email) {
        super(username, password, email);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}
