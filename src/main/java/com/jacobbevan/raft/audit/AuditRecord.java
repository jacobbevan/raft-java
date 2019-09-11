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
        SendLogUpdate,
        RecAppendEntries,
        StartElection,
        RecVote,
        DetectStaleTerm,
        AcceptVoteRequest,
        RejectVoteRequest,
        RecHeartbeatResponse,
        AcceptLogUpdate,
        RejectLogUpdate,
        RejectAppendEntries,
        RecVoteRequest,
        SyncroniseLogs,
        RecVoteRPCFailure,
        AppendEntriesRPCFailure
    }

    private String id;
    private RaftServer.RaftServerRole role;
    private AuditRecordType type;
    private int term;
    private String extraInfo;
    private Instant when;

    public AuditRecord(
            AuditRecordType type,
            String id,
            RaftServer.RaftServerRole role,
            int term,
            String extraInfo) {
        this.id = id;
        this.role = role;
        this.type = type;
        this.term = term;
        this.when = Instant.now();
        this.extraInfo = extraInfo;
    }

    public AuditRecord(
            AuditRecordType type,
            String id,
            RaftServer.RaftServerRole role,
            int term) {
        this(type, id, role, term, "");
    }

    public String getId() {
        return id;
    }

    public RaftServer.RaftServerRole getRole() {
        return role;
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
        return "When: " + when + " State: " + role + " Id: " + id + " Type: " + type + " Term: " + term + " ExtraInfo: " + extraInfo;
    }
}
