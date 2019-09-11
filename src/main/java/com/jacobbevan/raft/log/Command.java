package com.jacobbevan.raft.log;

public interface Command {
    void apply(State state);
}
