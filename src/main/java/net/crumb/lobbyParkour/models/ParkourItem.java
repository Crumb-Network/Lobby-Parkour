package net.crumb.lobbyParkour.models;

public class ParkourItem {
    private final String item;
    private final int amount;
    private final String name;

    public ParkourItem(String item, int amount, String name) {
        this.item = item;
        this.amount = amount;
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public String getItem() {
        return item;
    }

    public String getName() {
        return name;
    }
}
