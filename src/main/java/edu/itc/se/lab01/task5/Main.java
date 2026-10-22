package edu.itc.se.lab01.task5;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Fix A and Fix B: 8 threads x 100,000 transfers each ===");
        Runner.runFixed("OrderedBank (lock ordering)",
                new OrderedBank(Runner.ACCOUNTS, Runner.INITIAL));
        Runner.runFixed("TryLockBank (tryLock + back-off)",
                new TryLockBank(Runner.ACCOUNTS, Runner.INITIAL));

        System.out.println();
        System.out.println("=== Graceful shutdown: interrupt workers after 2 seconds ===");
        Runner.runUntilInterrupted("OrderedBank (pure CPU work, stops by checking the flag)",
                new OrderedBank(Runner.ACCOUNTS, Runner.INITIAL), 2000);
        Runner.runUntilInterrupted("TryLockBank (can also be interrupted inside tryLock/sleep)",
                new TryLockBank(Runner.ACCOUNTS, Runner.INITIAL), 2000);
    }
}
