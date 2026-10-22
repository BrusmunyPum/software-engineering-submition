package edu.itc.se.lab01.challenge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ReferenceWordCount {
    private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");

    private ReferenceWordCount() {
    }

    public static Map<String, Long> count(Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        Map<String, Long> counts = new HashMap<>();
        for (Path file : files) {
            for (String line : Files.readAllLines(file)) {
                for (String w : NON_LETTERS.split(line)) {
                    if (!w.isEmpty()) counts.merge(w.toLowerCase(), 1L, Long::sum);
                }
            }
        }
        return counts;
    }

    public static void main(String[] args) throws IOException {
        long t0 = System.nanoTime();
        Map<String, Long> counts = count(Path.of(args[0]));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf("reference: %d distinct words, %,d words in %d ms%n",
                counts.size(), total, (System.nanoTime() - t0) / 1_000_000);
    }
}
