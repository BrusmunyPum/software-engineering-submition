package edu.itc.se.lab01.task3;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Scenario.Result r = Scenario.run(new BoundedBuffer(5), true, 30_000);
        System.out.printf("Total processed: %d (expected %d) in %d ms%n", r.processed(), Scenario.EXPECTED, r.millis());
        if (!r.ok()) {
            throw new IllegalStateException("expected " + Scenario.EXPECTED + " but got " + r.processed());
        }

        System.out.println("--- repeating 20 runs quietly ---");
        for (int run = 1; run <= 20; run++) {
            Scenario.Result q = Scenario.run(new BoundedBuffer(5), false, 30_000);
            if (!q.ok()) {
                throw new IllegalStateException("run " + run + " failed: " + q);
            }
            System.out.printf("run %2d ok  processed=%d  %d ms%n", run, q.processed(), q.millis());
        }
        System.out.println("All 20 runs correct.");
    }
}
