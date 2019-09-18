package com.jacobbevan.raft.messages;

import com.jacobbevan.raft.log.LogEntry;

public class AppendEntriesCommand<C> {

    private int term;
    private String leaderId;
    private LogEntry<C> logEntry;
    private int prevLogIndex;
    private int prevLogTerm;
    private int maxCommit;

    public AppendEntriesCommand(String leaderId, int term, int maxCommit) {
        this.term = term;
        this.leaderId = leaderId;
    }

    public AppendEntriesCommand(String leaderId, int term, int maxCommit, LogEntry<C> logEntry,int prevLogIndex, int prevLogTerm) {
        this(leaderId, term, maxCommit);
        this.logEntry = logEntry;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
    }

    public boolean hasLogEntry() {
        return logEntry != null;
    }


    public int getTerm() {
        return term;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public int getMaxCommit() {
        return maxCommit;
    }

    public LogEntry<C> getLogEntry() {
        return logEntry;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public boolean hasPrevLogIndex() {
        return prevLogIndex != -1;
    }

    public boolean hasPrevLogTerm() {
        return prevLogTerm != -1;
    }
}
