package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;

public class Producer implements Runnable {
    private final OrderQueue queue;
    private final int firstId;
    private final int count;

    public Producer(OrderQueue queue, int firstId, int count) {
        this.queue = queue;
        this.firstId = firstId;
        this.count = count;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < count; i++) {
                int id = firstId + i;
                int qty = ThreadLocalRandom.current().nextInt(1, 6);
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 21));
                queue.put(new Order(id, "item-" + id, qty));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
