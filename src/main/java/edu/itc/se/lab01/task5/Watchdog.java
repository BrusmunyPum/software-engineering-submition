package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public final class Watchdog {

    private Watchdog() {
    }

    public static void start() {
        Thread watchdog = new Thread(() -> {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            try {
                while (true) {
                    Thread.sleep(1000);
                    long[] ids = mx.findDeadlockedThreads();
                    if (ids != null) {
                        StringBuilder names = new StringBuilder();
                        for (ThreadInfo info : mx.getThreadInfo(ids)) {
                            if (names.length() > 0) {
                                names.append(", ");
                            }
                            names.append(info.getThreadName());
                        }
                        System.out.println("[watchdog] DEADLOCK detected between: " + names);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }
}
