package com.jacobbevan.raft.messages;

public class RequestVoteResult {

    private String voterId;
    private boolean voteGranted;
    private int term;

    public RequestVoteResult(String voterId, boolean voteGranted, int term) {
        this.voterId = voterId;
        this.voteGranted = voteGranted;
        this.term = term;
    }

    public String getVoterId() {
        return voterId;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public int getTerm() {
        return term;
    }

}
