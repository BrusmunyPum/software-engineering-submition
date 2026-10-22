package edu.itc.se.lab01.task3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingOrderQueue implements OrderQueue {
    private final BlockingQueue<Order> q;

    public BlockingOrderQueue(int capacity) {
        this.q = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public void put(Order order) throws InterruptedException {
        q.put(order);
    }

    @Override
    public Order take() throws InterruptedException {
        return q.take();
    }
}
