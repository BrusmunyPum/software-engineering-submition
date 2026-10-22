package edu.itc.se.lab01.challenge;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WordCount {
    static final Pattern SPLIT = Pattern.compile("[^\\p{L}]+");
    private static final Path POISON = Path.of("__POISON__");
    private static final long SHUTDOWN_WAIT_MS = 2_000;

    private final BlockingQueue<Path> queue = new ArrayBlockingQueue<>(16);
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final AtomicInteger filesDone = new AtomicInteger();
    private final LongAdder wordsDone = new LongAdder();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final CountDownLatch workersDone;

    private final Path dir;
    private final int workers;
    private final boolean virtual;
    private final int top;
    private final long timeoutSec;
    private final Path csv;
    private final PrintStream out;

    private volatile int totalFiles;
    private volatile boolean partial;
    private long startNanos;
    private ExecutorService pool;
    private Thread scanner;
    private ScheduledExecutorService reporter;

    WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec) {
        this(dir, workers, virtual, top, timeoutSec, Path.of("result.csv"), System.out);
    }

    WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec, Path csv, PrintStream out) {
        if (workers < 1) throw new IllegalArgumentException("workers must be >= 1");
        this.dir = dir;
        this.workers = workers;
        this.virtual = virtual;
        this.top = top;
        this.timeoutSec = timeoutSec;
        this.csv = csv;
        this.out = out;
        this.workersDone = new CountDownLatch(workers);
    }

    void run() throws InterruptedException, IOException {
        startNanos = System.nanoTime();
        try (Stream<Path> files = Files.walk(dir)) {
            totalFiles = (int) files.filter(Files::isRegularFile).count();
        }

        pool = virtual
                ? Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())
                : Executors.newFixedThreadPool(workers, Thread.ofPlatform().name("worker-", 1).factory());
        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                try {
                    work();
                } finally {
                    workersDone.countDown();
                }
            });
        }
        scanner = Thread.ofPlatform().name("scanner").start(this::scan);
        reporter = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("reporter").factory());
        reporter.scheduleAtFixedRate(this::report, 500, 500, TimeUnit.MILLISECONDS);
        Thread hook = new Thread(() -> {
            if (!stopping.get()) out.printf("[%.1fs] Ctrl+C – stopping%n", elapsedSec());
            shutdown();
        }, "shutdown-hook");
        Runtime.getRuntime().addShutdownHook(hook);

        boolean finished = timeoutSec > 0
                ? workersDone.await(timeoutSec, TimeUnit.SECONDS)
                : awaitForever();

        if (finished && stopping.compareAndSet(false, true)) {
            reporter.shutdownNow();
            scanner.join();
            pool.close();
            printResult(false);
        } else if (!finished) {
            reporter.shutdownNow();
            reporter.awaitTermination(200, TimeUnit.MILLISECONDS);
            out.printf("[%.1fs] deadline reached – stopping%n", elapsedSec());
            shutdown();
        }

        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException jvmAlreadyExiting) {
        }
    }

    private boolean awaitForever() throws InterruptedException {
        workersDone.await();
        return true;
    }

    private void scan() {
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                queue.put(p);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (IOException | UncheckedIOException e) {
            out.println("[scanner] cannot walk " + dir + ": " + e.getMessage());
        }
        try {
            for (int i = 0; i < workers; i++) queue.put(POISON);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void work() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Path p = queue.take();
                if (p == POISON) return;
                countFile(p);
                if (Thread.currentThread().isInterrupted()) return;
                filesDone.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void countFile(Path p) {
        try (Stream<String> lines = Files.lines(p)) {
            lines.forEach(line -> {
                long n = 0;
                for (String w : SPLIT.split(line)) {
                    if (w.isEmpty()) continue;
                    counts.computeIfAbsent(w.toLowerCase(), k -> new LongAdder()).increment();
                    n++;
                }
                wordsDone.add(n);
            });
        } catch (IOException | UncheckedIOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                out.println("[" + Thread.currentThread().getName() + "] skipped " + p + ": " + e);
            }
        }
    }

    private void report() {
        long words = wordsDone.sum();
        double sec = elapsedSec();
        out.printf("[%.1fs] files %d/%d words %,d %,.0f w/s   top: %s%n",
                sec, filesDone.get(), totalFiles, words, words / sec, format(topEntries(3)));
    }

    void shutdown() {
        if (!stopping.compareAndSet(false, true)) return;
        partial = true;
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SHUTDOWN_WAIT_MS);
        if (reporter != null) reporter.shutdownNow();
        if (scanner != null) scanner.interrupt();
        if (pool != null) pool.shutdownNow();
        try {
            if (pool != null && !pool.awaitTermination(remainingMs(deadline), TimeUnit.MILLISECONDS)) {
                out.println("[shutdown] some workers did not stop within 2 s");
            }
            if (scanner != null) scanner.join(Math.max(1, remainingMs(deadline)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        printResult(true);
    }

    private static long remainingMs(long deadlineNanos) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()));
    }

    void printResult(boolean partial) {
        List<Map.Entry<String, Long>> sorted = sortedSnapshot();
        long total = wordsDone.sum();
        out.printf("%s files=%d words=%,d elapsed=%.2fs mode=%s%n",
                partial ? "[PARTIAL]" : "DONE", filesDone.get(), total, elapsedSec(), mode());
        out.printf("%5s %-12s %10s %8s%n", "rank", "word", "count", "share");
        for (int i = 0; i < Math.min(top, sorted.size()); i++) {
            var e = sorted.get(i);
            out.printf("%5d %-12s %,10d %7.2f %%%n", i + 1, e.getKey(), e.getValue(), share(e.getValue(), total));
        }
        try {
            writeCsv(sorted, total);
            out.println(csv + " written");
        } catch (IOException e) {
            out.println("could not write " + csv + ": " + e.getMessage());
        }
    }

    private void writeCsv(List<Map.Entry<String, Long>> sorted, long total) throws IOException {
        StringBuilder sb = new StringBuilder("rank,word,count,share\n");
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            sb.append(i + 1).append(',').append(e.getKey()).append(',').append(e.getValue()).append(',')
              .append(String.format("%.4f", share(e.getValue(), total))).append('\n');
        }
        Files.writeString(csv, sb);
    }

    private static double share(long count, long total) {
        return total == 0 ? 0 : 100.0 * count / total;
    }

    private static final Comparator<Map.Entry<String, Long>> BY_COUNT_DESC =
            Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());

    private List<Map.Entry<String, Long>> sortedSnapshot() {
        return snapshot().entrySet().stream().sorted(BY_COUNT_DESC).toList();
    }

    private List<Map.Entry<String, Long>> topEntries(int n) {
        return counts.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().sum()))
                .sorted(BY_COUNT_DESC)
                .limit(n)
                .toList();
    }

    private static String format(List<Map.Entry<String, Long>> entries) {
        StringBuilder sb = new StringBuilder();
        for (var e : entries) sb.append(String.format("%s(%,d) ", e.getKey(), e.getValue()));
        return sb.toString().trim();
    }

    Map<String, Long> snapshot() {
        Map<String, Long> copy = new HashMap<>();
        counts.forEach((w, n) -> copy.put(w, n.sum()));
        return copy;
    }

    boolean isPartial() {
        return partial;
    }

    int filesDone() {
        return filesDone.get();
    }

    String mode() {
        return (virtual ? "virtual(" : "platform(") + workers + ")";
    }

    private double elapsedSec() {
        return (System.nanoTime() - startNanos) / 1e9;
    }

    static WordCount fromArgs(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "usage: WordCount <dir> [--top N] [--timeout SECONDS] [--virtual] [--workers N] [--csv FILE]");
        }
        Path dir = Path.of(args[0]);
        int top = 10;
        long timeout = 0;
        boolean virtual = false;
        int workers = Runtime.getRuntime().availableProcessors();
        Path csv = Path.of("result.csv");
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--top" -> top = Integer.parseInt(args[++i]);
                case "--timeout" -> timeout = Long.parseLong(args[++i]);
                case "--virtual" -> virtual = true;
                case "--workers" -> workers = Integer.parseInt(args[++i]);
                case "--csv" -> csv = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        return new WordCount(dir, workers, virtual, top, timeout, csv, System.out);
    }

    public static void main(String[] args) throws Exception {
        WordCount wc;
        try {
            wc = fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        if (!Files.isDirectory(wc.dir)) {
            System.err.println("not a directory: " + wc.dir);
            return;
        }
        wc.run();
    }
}
