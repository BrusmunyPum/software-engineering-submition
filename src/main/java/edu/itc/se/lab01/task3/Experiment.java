package edu.itc.se.lab01.task3;

public class Experiment {

    public static void main(String[] args) throws InterruptedException {
        int trials = 20;
        int ok = 0, hung = 0, wrong = 0;
        for (int i = 1; i <= trials; i++) {
            Scenario.Result r = Scenario.run(new BrokenBuffer(5), false, 8_000);
            String verdict = r.timedOut() ? "HUNG" : (r.ok() ? "ok" : "WRONG COUNT");
            System.out.printf("trial %2d: %-11s processed=%d  %d ms%n", i, verdict, r.processed(), r.millis());
            if (r.timedOut()) hung++;
            else if (r.ok()) ok++;
            else wrong++;
        }
        System.out.printf("BrokenBuffer (notify + if): ok=%d hung=%d wrong=%d out of %d%n", ok, hung, wrong, trials);
    }
}
