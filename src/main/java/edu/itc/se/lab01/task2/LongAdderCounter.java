package edu.itc.se.lab01.task2;

import java.util.concurrent.atomic.LongAdder;

/*
 * Why LongAdder is faster under contention: AtomicLong has one variable, so all 8 threads
 * CAS the same memory location; most CAS attempts fail and retry, and that cache line keeps
 * bouncing between cores. LongAdder spreads the increments over several cells (roughly one
 * per contending thread), so threads rarely touch the same cell. sum() adds the cells when
 * the value is read. Measured here: AtomicCounter 180 ms, LongAdderCounter 29 ms.
 */
public class LongAdderCounter implements Counter {
    private final LongAdder count = new LongAdder();

    @Override
    public void increment() {
        count.increment();
    }

    @Override
    public long get() {
        return count.sum();
    }
}
