package com.jacobbevan.raft.log;

public class LogEntry<C> {

    private C command;
    private int term;
    private int index;

    public LogEntry(C command, int term, int index) {
        this.command = command;
        this.term = term;
        this.index = index;
    }

    public C getCommand() {
        return command;
    }

    public int getTerm() {
        return term;
    }

    public int getIndex() {
        return index;
    }
}
