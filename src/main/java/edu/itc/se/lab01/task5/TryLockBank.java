package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockBank implements TransferService {

    private static final class Account {
        final ReentrantLock lock = new ReentrantLock();
        long balance;

        Account(long balance) {
            this.balance = balance;
        }
    }

    private final Account[] accounts;
    private final LongAdder retries = new LongAdder();

    public TryLockBank(int count, long initialBalance) {
        accounts = new Account[count];
        for (int i = 0; i < count; i++) {
            accounts[i] = new Account(initialBalance);
        }
    }

    @Override
    public int accountCount() {
        return accounts.length;
    }

    @Override
    public void transfer(int fromIndex, int toIndex, long amount) throws InterruptedException {
        Account from = accounts[fromIndex];
        Account to = accounts[toIndex];
        while (true) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance >= amount) {
                                from.balance -= amount;
                                to.balance += amount;
                            }
                            return;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            retries.increment();
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
        }
    }

    @Override
    public long total() {
        long sum = 0;
        for (Account a : accounts) {
            a.lock.lock();
            try {
                sum += a.balance;
            } finally {
                a.lock.unlock();
            }
        }
        return sum;
    }

    @Override
    public long retries() {
        return retries.sum();
    }
}
