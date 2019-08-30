package com.jacobbevan.raft.audit;

import com.jacobbevan.raft.servers.RaftServer;

import java.time.Instant;

public final class AuditRecord {

    public enum AuditRecordType
    {
        BecomeCandidate,
        BecomeFollower,
        BecomeLeader,
        SendHeartbeat,
        RecAppendEntries,
        StartElection,
        RecVote,
        DetectStaleTerm,
        AcceptVoteRequest,
        RejectVoteRequest,
        RecHeartbeatResponse,
        RejectAppendEntries,
        RecVoteRequest,
        RecVoteRPCFailure,
        AppendEntriesRPCFailure
    }

    private String id;
    private RaftServer.RaftServerStateEnum state;
    private AuditRecordType type;
    private int term;
    private String extraInfo;
    private Instant when;

    public AuditRecord(
            AuditRecordType type,
            String id,
            RaftServer.RaftServerStateEnum state,
            int term,
            String extraInfo) {
        this.id = id;
        this.state = state;
        this.type = type;
        this.term = term;
        this.when = Instant.now();
        this.extraInfo = extraInfo;
    }

    public AuditRecord(
            AuditRecordType type,
            String id,
            RaftServer.RaftServerStateEnum state,
            int term) {
        this(type, id, state, term, "");
    }

    public String getId() {
        return id;
    }

    public RaftServer.RaftServerStateEnum getState() {
        return state;
    }

    public AuditRecordType getType() {
        return type;
    }

    public int getTerm() {
        return term;
    }

    public Instant getWhen() {
        return when;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    @Override
    public String toString() {
        return "When: " + when + " State: " + state + " Id: " + id + " Type: " + type + " Term: " + term + " ExtraInfo: " + extraInfo;
    }
}
