package edu.itc.se.lab01.task2;

import java.util.ArrayList;
import java.util.List;

public class Bench {
    static final int THREADS = 8;
    static final int ITERATIONS = 1_000_000;

    static void run(String label, Counter counter) throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    counter.increment();
                }
            }, "inc-" + t);
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) {
            th.join();
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        long expected = (long) THREADS * ITERATIONS;
        long actual = counter.get();
        System.out.printf("%-22s value=%,d  correct=%-5b  %d ms%n", label, actual, actual == expected, ms);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- UnsafeCounter, 3 runs (expect wrong totals) ---");
        for (int i = 1; i <= 3; i++) {
            run("UnsafeCounter #" + i, new UnsafeCounter());
        }
        System.out.println("--- correct versions ---");
        run("SynchronizedCounter", new SynchronizedCounter());
        run("LockCounter", new LockCounter());
        run("AtomicCounter", new AtomicCounter());
        run("LongAdderCounter", new LongAdderCounter());
    }
}
