package edu.itc.se.lab01.task2;

/*
 * Lost updates recorded on my machine (8 threads x 1,000,000 increments, expected 8,000,000):
 *   run 1: 3,384,995   run 2: 4,531,892   run 3: 8,000,000 (correct by luck, the race is intermittent)
 * count++ is read, add, write: two threads can read the same value and one increment is lost.
 */
public class UnsafeCounter implements Counter {
    private long count;

    @Override
    public void increment() {
        count++;
    }

    @Override
    public long get() {
        return count;
    }
}
