package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.servers.SchedulePlanner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestSchedulePlanner {

    private SchedulePlanner planner;
    private final int retryDelay = 50;
    private Exception ex;
    private boolean completed = false;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    @Before
    public void setupPlanner() {
        this.planner = new SchedulePlanner(1000,1000,1000, this.retryDelay);
        this.ex = null;
        this.completed = false;
    }

    @Test
    public void general_delay_with_callback() throws InterruptedException {

        this.lock.lock();
        try {
            var startMs = System.currentTimeMillis();
            var delay = planner.delay(this.retryDelay, ()-> setComplete());

            while(!this.completed) {
                condition.await();
            }

            var elapsed = System.currentTimeMillis() - startMs;
            Assert.assertTrue(elapsed > this.retryDelay);
            Assert.assertTrue(elapsed < 2 * this.retryDelay);
            Assert.assertTrue(this.completed);

        }
        finally {
            this.lock.unlock();
        }
    }

    @Test
    public void delay_with_cancellation() throws Exception{

        var startMs = System.currentTimeMillis();
        var delay = planner.delay(this.retryDelay, ()-> setComplete());
        delay.close();
        Thread.sleep(this.retryDelay*4);
        var elapsed = System.currentTimeMillis() - startMs;
        Assert.assertTrue(elapsed > this.retryDelay);
        Assert.assertFalse(this.completed);
    }

    @Test
    public void retryDelay_with_call_back() throws InterruptedException {

        this.lock.lock();
        try {
            var startMs = System.currentTimeMillis();
            var delay = planner.retryDelay(()-> setComplete());

            while(!this.completed) {
                condition.await();
            }

            var elapsed = System.currentTimeMillis() - startMs;
            Assert.assertTrue(elapsed > this.retryDelay);
            Assert.assertTrue(elapsed < 2 * this.retryDelay);
            Assert.assertTrue(this.completed);

        }
        finally {
            this.lock.unlock();
        }
    }

    public void setComplete() {

        this.lock.lock();
        try {
            this.completed =  true;
            this.ex = null;
            this.condition.signal();

        }
        finally {
            this.lock.unlock();
        }
    }
}
