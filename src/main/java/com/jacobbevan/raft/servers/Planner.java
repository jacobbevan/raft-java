package com.jacobbevan.raft.servers;

public interface Planner {

    SafeAutoCloseable heartbeatDelay(Runnable callback);

    SafeAutoCloseable electionDelay(Runnable callback);

    SafeAutoCloseable retryDelay(Runnable callback);

    SafeAutoCloseable delay(int delayMillis, Runnable callback);

}
