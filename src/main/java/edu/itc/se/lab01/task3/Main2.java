package edu.itc.se.lab01.task3;

public class Main2 {

    public static void main(String[] args) throws InterruptedException {
        Scenario.Result r = Scenario.run(new BlockingOrderQueue(5), true, 30_000);
        System.out.printf("Total processed: %d (expected %d) in %d ms%n", r.processed(), Scenario.EXPECTED, r.millis());
        if (!r.ok()) {
            throw new IllegalStateException("expected " + Scenario.EXPECTED + " but got " + r.processed());
        }
    }
}
