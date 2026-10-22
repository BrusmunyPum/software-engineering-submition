package edu.itc.se.lab01.task3;

public record Order(int id, String item, int qty) {
    public static final int POISON_ID = -1;

    public static Order poisonPill() {
        return new Order(POISON_ID, "poison", 0);
    }

    public boolean isPoisonPill() {
        return id == POISON_ID;
    }
}
