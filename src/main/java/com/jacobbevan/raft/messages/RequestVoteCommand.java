package com.jacobbevan.raft.messages;

public class RequestVoteCommand {

    private String candidateId;
    private int currentTerm;

    public RequestVoteCommand(String candidateId, int currentTerm) {
        this.candidateId = candidateId;
        this.currentTerm = currentTerm;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }
}
