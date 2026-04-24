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
        System.out.println("[Ngh?? thu??????t] " + getName() + " c????a t??c gi?????? " + artistName + " - Gi?? hi??n t????i: " + getCurrentHighestPrice());
    }

    public String getArtistName() {
        return artistName;
    }
}
