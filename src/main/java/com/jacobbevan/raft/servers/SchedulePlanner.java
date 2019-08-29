package com.jacobbevan.raft.servers;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulePlanner implements Planner, AutoCloseable {

    private final Random random;
    private final ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor();

    private final int electionTimerMeanMs;
    private final int electionTimerRangeMs;
    private final int heartBeatIntervalMs;
    private final int retryIntervalMs;

    public SchedulePlanner(int electionTimerMeanMs, int electionTimerRangeMs, int heartBeatIntervalMs, int retryIntervalMs) {

        this.random = new Random();
        this.electionTimerMeanMs = electionTimerMeanMs;
        this.electionTimerRangeMs = electionTimerRangeMs;
        this.heartBeatIntervalMs = heartBeatIntervalMs;
        this.retryIntervalMs = retryIntervalMs;
    }

    @Override
    public SafeAutoCloseable heartbeatDelay(Runnable callback) {
        return delay(this.heartBeatIntervalMs, callback);
    }

    @Override
    public SafeAutoCloseable  electionDelay(Runnable callback) {
        var heartbeatDelay = this.random.nextInt(this.electionTimerRangeMs) + this.electionTimerMeanMs;
        return delay(heartbeatDelay, callback);
    }

    @Override
    public SafeAutoCloseable  retryDelay(Runnable callback) {
        return delay(this.retryIntervalMs, callback);
    }

    @Override
    public SafeAutoCloseable  delay(int delayMillis, Runnable callback) {

        var future = timerService.schedule(
                callback,
                delayMillis,
                TimeUnit.MILLISECONDS);


        var cancel = new SafeAutoCloseable () {
            public void close() {
                future.cancel(false);
            }
        };

        return cancel;
    }

    @Override
    public void close() {

        this.timerService.shutdown();
        try {
            if (!this.timerService.awaitTermination(1, TimeUnit.SECONDS)) {
                this.timerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.timerService.shutdownNow();
        }
    }
}
