package com.jacobbevan.raft.messages;

public class AppendEntriesCommand {

    private int term;
    private String leaderId;

    public AppendEntriesCommand(String leaderId, int term) {
        this.term = term;
        this.leaderId = leaderId;
    }

    public int getTerm() {
        return term;
    }

    public String getLeaderId() {
        return leaderId;
    }
}
