package edu.itc.se.lab01.challenge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GenerateData {
    static final int VOCABULARY_SIZE = 2_000;
    private static final int WORDS_PER_LINE = 12;
    private static final String[] COMMON = {
            "the", "and", "of", "to", "a", "in", "is", "it", "you", "that", "he", "was", "for", "on", "are",
            "with", "as", "his", "they", "be", "at", "one", "have", "this", "from", "or", "had", "by", "word",
            "but", "what", "some", "we", "can", "out", "other", "were", "all", "there", "when", "up", "use",
            "your", "how", "said", "an", "each", "she", "which", "do", "their", "time", "if", "will", "way"
    };
    private static final String[] SYLLABLES = {
            "ka", "lo", "mi", "ra", "te", "su", "no", "vi", "da", "pe", "zu", "ri", "ma", "to", "ne", "sa",
            "bo", "li", "ku", "fe"
    };

    private GenerateData() {
    }

    static List<String> vocabulary() {
        List<String> vocab = new ArrayList<>(List.of(COMMON));
        for (int i = 0; vocab.size() < VOCABULARY_SIZE; i++) {
            StringBuilder w = new StringBuilder();
            int n = i;
            do {
                w.append(SYLLABLES[n % SYLLABLES.length]);
                n /= SYLLABLES.length;
            } while (n > 0);
            w.append(SYLLABLES[i % 7]);
            String word = w.toString();
            if (!vocab.contains(word)) vocab.add(word);
        }
        return vocab;
    }

    public static void generate(Path dir, int files, int wordsPerFile, long seed) throws IOException {
        Files.createDirectories(dir);
        List<String> vocab = vocabulary();
        double[] cumulative = new double[vocab.size()];
        double sum = 0;
        for (int k = 0; k < vocab.size(); k++) {
            sum += 1.0 / (k + 1);
            cumulative[k] = sum;
        }
        Random rnd = new Random(seed);
        for (int f = 0; f < files; f++) {
            Path file = dir.resolve(String.format("file-%05d.txt", f));
            try (BufferedWriter out = Files.newBufferedWriter(file)) {
                for (int i = 0; i < wordsPerFile; i++) {
                    int k = pick(cumulative, rnd.nextDouble() * sum);
                    String word = vocab.get(k);
                    boolean lineStart = i % WORDS_PER_LINE == 0;
                    if (lineStart && i > 0) out.newLine();
                    if (!lineStart) out.write(' ');
                    out.write(lineStart ? Character.toUpperCase(word.charAt(0)) + word.substring(1) : word);
                    if (i % WORDS_PER_LINE == WORDS_PER_LINE - 1) out.write('.');
                    else if (rnd.nextInt(15) == 0) out.write(',');
                }
                out.newLine();
            }
        }
    }

    private static int pick(double[] cumulative, double x) {
        int lo = 0, hi = cumulative.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cumulative[mid] < x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: GenerateData <dir> [--files 200] [--words 50000] [--seed 42]");
            return;
        }
        Path dir = Path.of(args[0]);
        int files = 200, words = 50_000;
        long seed = 42;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--files" -> files = Integer.parseInt(args[++i]);
                case "--words" -> words = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        long t0 = System.nanoTime();
        generate(dir, files, words, seed);
        System.out.printf("wrote %d files x %,d words to %s in %d ms%n",
                files, words, dir, (System.nanoTime() - t0) / 1_000_000);
    }
}
