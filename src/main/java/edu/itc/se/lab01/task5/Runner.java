package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;

public final class Runner {
    public static final int WORKERS = 8;
    public static final int ACCOUNTS = 4;
    public static final long INITIAL = 1_000;
    public static final int TRANSFERS_PER_WORKER = 100_000;

    private Runner() {
    }

    public static void runFixed(String label, TransferService bank) throws InterruptedException {
        long start = System.nanoTime();
        long[] done = new long[WORKERS];
        Thread[] workers = new Thread[WORKERS];
        for (int w = 0; w < WORKERS; w++) {
            int index = w;
            workers[w] = Thread.ofPlatform().name("transfer-" + w).start(() -> {
                var rnd = ThreadLocalRandom.current();
                int n = bank.accountCount();
                try {
                    for (int i = 0; i < TRANSFERS_PER_WORKER; i++) {
                        int a = rnd.nextInt(n);
                        int b = (a + rnd.nextInt(1, n)) % n;
                        bank.transfer(a, b, rnd.nextInt(1, 50));
                        done[index]++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        long transfers = 0;
        for (int w = 0; w < WORKERS; w++) {
            workers[w].join();
            transfers += done[w];
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        long expected = ACCOUNTS * INITIAL;
        System.out.printf("# %s%n  transfers=%d retries=%d total=%d (expected %d) in %d ms  -> %s%n",
                label, transfers, bank.retries(), bank.total(), expected, ms,
                bank.total() == expected ? "OK" : "BROKEN");
    }

    public static void runUntilInterrupted(String label, TransferService bank, long runMillis)
            throws InterruptedException {
        long[] done = new long[WORKERS];
        Thread[] workers = new Thread[WORKERS];
        for (int w = 0; w < WORKERS; w++) {
            int index = w;
            workers[w] = Thread.ofPlatform().name("transfer-" + w).start(() -> {
                var rnd = ThreadLocalRandom.current();
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        int a = rnd.nextInt(bank.accountCount());
                        int b = rnd.nextInt(bank.accountCount());
                        if (a != b) {
                            bank.transfer(a, b, rnd.nextInt(1, 50));
                            done[index]++;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(runMillis);
        System.out.printf("# %s%n[main] interrupting %d workers after %d ms%n", label, WORKERS, runMillis);
        long t0 = System.nanoTime();
        for (Thread t : workers) {
            t.interrupt();
        }
        boolean allStopped = true;
        long sumTransfers = 0;
        for (int w = 0; w < WORKERS; w++) {
            workers[w].join(1000);
            boolean alive = workers[w].isAlive();
            allStopped &= !alive;
            sumTransfers += done[w];
            System.out.printf("  %s stopped after %,d transfers (alive=%b)%n", workers[w].getName(), done[w], alive);
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long expected = ACCOUNTS * INITIAL;
        System.out.printf("  all workers stopped=%b within %d ms, transfers=%,d, total=%d (expected %d)%n",
                allStopped, ms, sumTransfers, bank.total(), expected);
    }
}
