package com.jacobbevan.raft.messages;

public class AppendEntriesResult {

    private boolean success;
    private int term;

    public AppendEntriesResult(boolean success, int term) {
        this.success = success;
        this.term = term;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTerm() {
        return term;
    }

}
