package edu.itc.se.lab01.task5;

public class OrderedBank implements TransferService {

    private static final class Account {
        final int id;
        long balance;

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    private final Account[] accounts;

    public OrderedBank(int count, long initialBalance) {
        accounts = new Account[count];
        for (int i = 0; i < count; i++) {
            accounts[i] = new Account(i, initialBalance);
        }
    }

    @Override
    public int accountCount() {
        return accounts.length;
    }

    @Override
    public void transfer(int fromIndex, int toIndex, long amount) {
        Account from = accounts[fromIndex];
        Account to = accounts[toIndex];
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    @Override
    public long total() {
        long sum = 0;
        for (Account a : accounts) {
            synchronized (a) {
                sum += a.balance;
            }
        }
        return sum;
    }
}
