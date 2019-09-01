package com.jacobbevan.raft.mocks;

import com.jacobbevan.raft.audit.AuditLogger;
import com.jacobbevan.raft.audit.AuditRecord;

import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class MonitoringAuditLogger <T> implements AuditLogger {

    private Object lock;
    private CountDownLatch latch = new CountDownLatch(1);
    private BiFunction<T,AuditRecord,T> aggregator;
    private Predicate<T> completion;
    private T value;

    private MonitoringAuditLogger(BiFunction<T, AuditRecord, T> aggregator, Predicate<T> completion, T initialValue) {
        this.aggregator = aggregator;
        this.completion = completion;
        this.value = initialValue;
    }

    @Override
    public void Log(AuditRecord record) {


        synchronized (lock) {

            this.value = aggregator.apply(this.value, record);

            if(this.completion.test(this.value)) {
                latch.countDown();
            }
        }
    }

    public void await() throws InterruptedException {
        latch.await();
    }


    //TODO add facotry methods
    //public static MonitoringAuditLogger
}
