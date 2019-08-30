package com.jacobbevan.raft.messages;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestVoteCommand that = (RequestVoteCommand) o;
        return currentTerm == that.currentTerm &&
                Objects.equals(candidateId, that.candidateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateId, currentTerm);
    }
}
