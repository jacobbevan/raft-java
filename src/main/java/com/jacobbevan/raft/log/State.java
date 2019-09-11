package com.jacobbevan.raft.log;

public interface State<C> {
    void apply(C command);
}
