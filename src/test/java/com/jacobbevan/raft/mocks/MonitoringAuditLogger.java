package com.jacobbevan.raft.mocks;

import com.jacobbevan.raft.audit.AuditLogger;
import com.jacobbevan.raft.audit.AuditRecord;
import com.jacobbevan.raft.servers.RaftServer;

import javax.management.monitor.Monitor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MonitoringAuditLogger <T> implements AwaitableAuditLogger {

    private Object lock = new Object();
    private CountDownLatch latch = new CountDownLatch(1);
    private BiFunction<T,AuditRecord,T> aggregator;
    private Predicate<T> completion;
    private T value;
    private AuditLogger inner;
    private boolean completed = false;

    public MonitoringAuditLogger(AuditLogger inner, BiFunction<T, AuditRecord, T> aggregator, Predicate<T> completion, T initialValue) {
        this.aggregator = aggregator;
        this.completion = completion;
        this.value = initialValue;
        this.inner = inner;
    }

    @Override
    public void Log(AuditRecord record) {

        synchronized (lock) {
            inner.Log(record);
            if(!completed) {
                value = aggregator.apply(value, record);

                if(completion.test(value)) {
                    latch.countDown();
                    completed = true;
                }
            }
        }
    }

    public void await() throws InterruptedException {
        latch.await();
    }

    public void reset() {
        synchronized (lock) {
            latch = new CountDownLatch(1);
            completed = false;
        }

    }
}
