package network;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionNotificationManager implements AuctionSubject {
    private static AuctionNotificationManager instance;
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private AuctionNotificationManager() {}

    public static synchronized AuctionNotificationManager getInstance() {
        if (instance == null) {
            instance = new AuctionNotificationManager();
        }
        return instance;
    }

    @Override
    public void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyAllObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.updateClient(message);
        }
    }
}
