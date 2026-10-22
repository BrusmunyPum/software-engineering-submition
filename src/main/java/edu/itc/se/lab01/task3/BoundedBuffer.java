package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;

public class BoundedBuffer implements OrderQueue {
    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public synchronized void put(Order order) throws InterruptedException {
        while (items.size() == capacity) {
            wait();
        }
        items.addLast(order);
        if (items.size() > capacity) {
            throw new IllegalStateException("buffer exceeded capacity: " + items.size());
        }
        notifyAll();
    }

    @Override
    public synchronized Order take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("take from an empty buffer");
        }
        Order order = items.removeFirst();
        notifyAll();
        return order;
    }
}
