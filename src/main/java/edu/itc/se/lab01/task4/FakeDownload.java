package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FakeDownload implements Callable<String> {
    public static final int COUNT = 500;
    public static final int SLOW_INDEX = 0;
    public static final long NORMAL_MS = 200;
    public static final long SLOW_MS = 5_000;

    private final String url;
    private final long sleepMs;
    private final boolean fail;

    public FakeDownload(String url, long sleepMs, boolean fail) {
        this.url = url;
        this.sleepMs = sleepMs;
        this.fail = fail;
    }

    public String url() {
        return url;
    }

    @Override
    public String call() throws IOException, InterruptedException {
        Thread.sleep(sleepMs);
        if (fail) {
            throw new IOException("boom");
        }
        return url + " -> " + url.length();
    }

    public static List<FakeDownload> create(boolean withSlowOne) {
        List<FakeDownload> list = new ArrayList<>();
        for (int id = 1; id <= COUNT; id++) {
            boolean slow = withSlowOne && id == SLOW_INDEX + 1;
            boolean fail = id % 50 == 0;
            list.add(new FakeDownload("https://example.com/file-" + id, slow ? SLOW_MS : NORMAL_MS, fail));
        }
        return list;
    }
}
