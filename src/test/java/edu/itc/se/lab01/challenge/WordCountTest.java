package edu.itc.se.lab01.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WordCountTest {

    @TempDir
    static Path tmp;
    static Path fixture;
    static Map<String, Long> expected;

    @BeforeAll
    static void createFixture() throws Exception {
        fixture = tmp.resolve("fixture");
        GenerateData.generate(fixture, 20, 20_000, 7);
        expected = ReferenceWordCount.count(fixture);
    }

    private static WordCount quiet(Path dir, int workers, boolean virtual, long timeoutSec) {
        PrintStream sink = new PrintStream(new ByteArrayOutputStream());
        return new WordCount(dir, workers, virtual, 5, timeoutSec, tmp.resolve("result.csv"), sink);
    }

    @Test
    void parallelPlatformMatchesReference() throws Exception {
        WordCount wc = quiet(fixture, 4, false, 0);
        wc.run();
        assertFalse(wc.isPartial());
        assertEquals(20, wc.filesDone());
        assertEquals(expected, wc.snapshot());
    }

    @Test
    void virtualThreadsMatchReference() throws Exception {
        WordCount wc = quiet(fixture, 8, true, 0);
        wc.run();
        assertEquals(expected, wc.snapshot());
    }

    @Test
    void singleWorkerMatchesReference() throws Exception {
        WordCount wc = quiet(fixture, 1, false, 0);
        wc.run();
        assertEquals(expected, wc.snapshot());
    }

    @Test
    void csvIsWritten() throws Exception {
        WordCount wc = quiet(fixture, 2, false, 0);
        wc.run();
        var lines = Files.readAllLines(tmp.resolve("result.csv"));
        assertEquals("rank,word,count,share", lines.get(0));
        assertTrue(lines.get(1).startsWith("1,the,"), lines.get(1));
        assertEquals(expected.size() + 1, lines.size());
    }

    @Test
    void stopsWithinThreeSecondsOnTimeout() throws Exception {
        Path big = tmp.resolve("big");
        GenerateData.generate(big, 5_000, 2_000, 1);

        WordCount wc = WordCount.fromArgs(new String[] {
                big.toString(), "--timeout", "1", "--workers", "2", "--csv", tmp.resolve("partial.csv").toString()
        });
        long t0 = System.nanoTime();
        wc.run();
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertTrue(ms < 3_000, "took " + ms + " ms");
        assertTrue(wc.isPartial(), "expected a [PARTIAL] result");
        assertTrue(wc.filesDone() < 5_000);
        boolean stray = Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.isAlive() && (t.getName().equals("scanner")
                        || t.getName().startsWith("worker-") || t.getName().equals("reporter")));
        assertFalse(stray, "pipeline threads still alive after run()");
    }
}
