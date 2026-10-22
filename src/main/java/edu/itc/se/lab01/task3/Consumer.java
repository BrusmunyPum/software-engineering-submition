package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private final OrderQueue queue;
    private final AtomicInteger processed;
    private final boolean verbose;

    public Consumer(OrderQueue queue, AtomicInteger processed, boolean verbose) {
        this.queue = queue;
        this.processed = processed;
        this.verbose = verbose;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        try {
            while (true) {
                Order order = queue.take();
                if (order.isPoisonPill()) {
                    if (verbose) {
                        System.out.println(name + " received poison pill, stopping");
                    }
                    return;
                }
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 31));
                processed.incrementAndGet();
                if (verbose) {
                    System.out.println(name + " processed " + order);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
