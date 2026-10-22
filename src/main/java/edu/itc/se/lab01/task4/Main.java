package edu.itc.se.lab01.task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    static final long LIMIT = 4_000_000;
    static final int CHUNKS = 16;

    record Summary(int ok, int failed, int timedOut, String firstFailure) {
    }

    public static void main(String[] args) throws Exception {
        primeDemo();
        System.out.println();
        downloadDemo("fixed(20) platform threads", Executors.newFixedThreadPool(20));
        downloadDemo("virtual threads", Executors.newVirtualThreadPerTaskExecutor());
        System.out.println();
        completableFutureDemo();
        System.out.println("main finished, JVM will exit by itself");
    }

    static void primeDemo() throws Exception {
        long t0 = System.nanoTime();
        long single = PrimeCounter.countPrimes(0, LIMIT);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, elapsedMs(t0));

        int threads = Runtime.getRuntime().availableProcessors();
        t0 = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < CHUNKS; i++) {
                long from = i * LIMIT / CHUNKS;
                long to = (i + 1) * LIMIT / CHUNKS;
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }
            long total = 0;
            for (Future<Long> f : futures) {
                total += f.get();
            }
            System.out.printf("pool (%d threads): %d primes in %d ms  (same as single: %b)%n",
                    threads, total, elapsedMs(t0), total == single);
        }
    }

    static void downloadDemo(String label, ExecutorService executor) throws InterruptedException {
        long t0 = System.nanoTime();
        Summary s;
        try (executor) {
            List<Future<String>> futures = new ArrayList<>();
            for (FakeDownload d : FakeDownload.create(true)) {
                futures.add(executor.submit(d));
            }
            s = collect(futures);
        }
        System.out.printf("downloads on %-28s: ok=%d failed=%d timedOut=%d in %d ms%n",
                label, s.ok(), s.failed(), s.timedOut(), elapsedMs(t0));
        System.out.println("    example failure cause: " + s.firstFailure());
    }

    static Summary collect(List<Future<String>> futures) throws InterruptedException {
        int ok = 0, failed = 0, timedOut = 0;
        String firstFailure = "none";
        for (int i = 0; i < futures.size(); i++) {
            Future<String> f = futures.get(i);
            try {
                if (i == FakeDownload.SLOW_INDEX) {
                    f.get(1, TimeUnit.SECONDS);
                } else {
                    f.get();
                }
                ok++;
            } catch (ExecutionException e) {
                failed++;
                if (firstFailure.equals("none")) {
                    firstFailure = e.getCause().toString();
                }
            } catch (TimeoutException e) {
                f.cancel(true);
                timedOut++;
            }
        }
        return new Summary(ok, failed, timedOut, firstFailure);
    }

    static void completableFutureDemo() {
        long t0 = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> all = new ArrayList<>();
            for (FakeDownload d : FakeDownload.create(false)) {
                CompletableFuture<String> cf = CompletableFuture
                        .supplyAsync(() -> run(d), executor)
                        .handle((value, error) -> error == null ? value : "fallback for " + d.url());
                all.add(cf);
            }
            CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).join();
            long fallbacks = all.stream().filter(cf -> cf.join().startsWith("fallback")).count();
            System.out.printf("CompletableFuture on virtual threads: normal=%d fallback=%d in %d ms%n",
                    all.size() - fallbacks, fallbacks, elapsedMs(t0));
        }
    }

    static String run(FakeDownload d) {
        try {
            return d.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
