package models;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artistName;

    public Art(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String artistName) {
        super(name, description, startingPrice, startTime, endTime);
        this.artistName = artistName;
    }

    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + getName() + " của tác giả " + artistName + " - Giá hiện tại: " + getCurrentHighestPrice());
    }

    public String getArtistName() {
        return artistName;
    }
}