/*
 * run() vs start() -- what the --sequential flag shows
 *
 * t.start() creates a new thread, and that new thread calls run(). The four countdowns
 * therefore run at the same time, and "Mission control: all launched" prints immediately,
 * before any countdown has finished.
 *
 * t.run() is only a normal method call: it executes on the current thread (main). Nothing
 * runs in parallel, so each countdown finishes before the next one starts, and
 * "all launched" prints only after all four. gemini, mercury and artemis print "main: 3"
 * because CountdownTask uses Thread.currentThread(), which is main; apollo still prints
 * "apollo" because Countdown uses its own getName(). The Thread objects are never started,
 * so join() returns immediately and getState() stays NEW instead of TERMINATED.
 */
package edu.itc.se.lab01.task1;

import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        boolean sequential = args.length > 0 && args[0].equals("--sequential");

        Thread apollo = new Countdown("apollo");
        Thread gemini = new Thread(new CountdownTask(), "gemini");
        Thread mercury = new Thread(() -> new CountdownTask().run(), "mercury");
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        List<Thread> threads = List.of(apollo, gemini, mercury, artemis);

        for (Thread t : threads) {
            if (sequential) {
                t.run();
            } else {
                t.start();
            }
        }
        System.out.println("Mission control: all launched");

        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Mission control: all landed");

        for (Thread t : threads) {
            System.out.printf("%-8s virtual=%-5b state=%s%n", t.getName(), t.isVirtual(), t.getState());
        }
    }
}
