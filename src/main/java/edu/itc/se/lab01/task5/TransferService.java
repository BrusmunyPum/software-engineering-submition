package edu.itc.se.lab01.task5;

public interface TransferService {
    int accountCount();

    void transfer(int fromIndex, int toIndex, long amount) throws InterruptedException;

    long total();

    default long retries() {
        return 0;
    }
}
