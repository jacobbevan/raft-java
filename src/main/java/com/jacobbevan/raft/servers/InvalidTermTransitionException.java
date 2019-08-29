package com.jacobbevan.raft.servers;

public class InvalidTermTransitionException extends RuntimeException {

    public InvalidTermTransitionException () {}

    public InvalidTermTransitionException (String message) {
        super(message);
    }
}
