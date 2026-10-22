package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;

public class BrokenBuffer implements OrderQueue {
    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BrokenBuffer(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public synchronized void put(Order order) throws InterruptedException {
        if (items.size() == capacity) {
            wait();
        }
        items.addLast(order);
        notify();
    }

    @Override
    public synchronized Order take() throws InterruptedException {
        if (items.isEmpty()) {
            wait();
        }
        Order order = items.removeFirst();
        notify();
        return order;
    }
}
