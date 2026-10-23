package net.crumb.lobbyParkour.models;

public class ParkourItem {
    private final String item;
    private final int amount;
    private final int slot;
    private final String name;

    public ParkourItem(String item, int amount, int slot, String name) {
        this.item = item;
        this.amount = amount;
        this.slot = slot;
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public int getSlot() {
        return slot;
    }

    public String getItem() {
        return item;
    }

    public String getName() {
        return name;
    }

    /** An amount of 0 (or less) means the item is disabled and never given to the player. */
    public boolean isEnabled() {
        return amount > 0;
    }
}
