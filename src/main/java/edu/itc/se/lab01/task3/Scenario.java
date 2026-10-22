package edu.itc.se.lab01.task3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Scenario {
    public static final int PRODUCERS = 2;
    public static final int ORDERS_PER_PRODUCER = 50;
    public static final int CONSUMERS = 3;
    public static final int EXPECTED = PRODUCERS * ORDERS_PER_PRODUCER;

    public record Result(int processed, boolean timedOut, long millis) {
        public boolean ok() {
            return !timedOut && processed == EXPECTED;
        }
    }

    public static Result run(OrderQueue queue, boolean verbose, long timeoutMs) throws InterruptedException {
        long start = System.nanoTime();
        long deadline = System.currentTimeMillis() + timeoutMs;
        AtomicInteger processed = new AtomicInteger();

        List<Thread> producers = new ArrayList<>();
        for (int p = 0; p < PRODUCERS; p++) {
            int firstId = p * ORDERS_PER_PRODUCER + 1;
            producers.add(Thread.ofPlatform().name("producer-" + (p + 1))
                    .start(new Producer(queue, firstId, ORDERS_PER_PRODUCER)));
        }
        List<Thread> consumers = new ArrayList<>();
        for (int c = 0; c < CONSUMERS; c++) {
            consumers.add(Thread.ofPlatform().name("consumer-" + (c + 1))
                    .start(new Consumer(queue, processed, verbose)));
        }

        boolean timedOut = !joinAll(producers, deadline);
        if (!timedOut) {
            for (int c = 0; c < CONSUMERS; c++) {
                queue.put(Order.poisonPill());
            }
            timedOut = !joinAll(consumers, deadline);
        }
        if (timedOut) {
            producers.forEach(Thread::interrupt);
            consumers.forEach(Thread::interrupt);
            joinAll(producers, System.currentTimeMillis() + 1000);
            joinAll(consumers, System.currentTimeMillis() + 1000);
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        return new Result(processed.get(), timedOut, ms);
    }

    private static boolean joinAll(List<Thread> threads, long deadline) throws InterruptedException {
        for (Thread t : threads) {
            long left = deadline - System.currentTimeMillis();
            if (left > 0) {
                t.join(left);
            }
            if (t.isAlive()) {
                return false;
            }
        }
        return true;
    }
}
