package com.jacobbevan.raft.servers;

import com.jacobbevan.raft.audit.AuditLogger;
import com.jacobbevan.raft.audit.AuditRecord;
import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RaftServer implements Server {

    public enum RaftServerStateEnum {
        Leader,
        Follower,
        Candidate
    }

    private static Logger logger = LoggerFactory.getLogger(RaftServer.class);

    private final static int INITIAL_TERM = 0;
    private final String id;
    private final Planner planner;
    private final AuditLogger auditLog;

    private RaftServerStateEnum state;
    private int currentTerm;
    private String votedFor;
    private int votesReceived = 0;
    private List<ServerProxy> servers = new ArrayList<>();
    private SafeAutoCloseable electionTimer;
    private SafeAutoCloseable heartbeatTimer;

    public RaftServer(String id, Planner planner, AuditLogger auditLog) {
        this.id = id;
        this.planner = planner;
        this.auditLog = auditLog;
        becomeFollower(INITIAL_TERM);
    }

    @Override
    public synchronized String getId() {
        //TODO thread safety
        return id;
    }

    @Override
    public synchronized int getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public synchronized RaftServerStateEnum getState() {
        return state;
    }


    @Override
    public synchronized void initialise(Collection<ServerProxy> servers) {
        //TODO replace with server discover
        for (ServerProxy server : servers) {
            if(server.getId()!= id) {
                this.servers.add((server));
            }
        }
    }

    @Override
    public synchronized AppendEntriesResult appendEntries(AppendEntriesCommand request) {

        this.auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.RecAppendEntries, this.id, this.state, this.currentTerm));

        //this instance considers sender's Term to be stale - reject request and complete.

        //NOT IMPLEMENTED return false if log doesn't contain an entry.
        //NOT IMPLEMENTED if conflict, delete existing entry

        if(this.currentTerm > request.getTerm()) {
            this.auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.RejectAppendEntries, this.id, this.state, this.currentTerm));
            return new AppendEntriesResult(false, this.currentTerm);
        }

        //we should become a follower, irrespective of current state
        becomeFollower(request.getTerm());

        return new AppendEntriesResult(true, this.currentTerm);
    }

    @Override
    public synchronized RequestVoteResult requestVote(RequestVoteCommand request) {

        this.auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.RecVoteRequest, this.id, this.state, this.currentTerm, "Requestor: " + request.getCandidateId()));

        becomeFollowerIfTermIsStale(request.getCurrentTerm());

        if((this.votedFor == request.getCandidateId() || this.votedFor == null) && request.getCurrentTerm() >= this.currentTerm)
        {
            auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.AcceptVoteRequest, this.id, this.state, this.currentTerm));
            this.votedFor = request.getCandidateId();
            return new RequestVoteResult (this.id, true, this.currentTerm);
        }
        else
        {
            auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.RejectVoteRequest, this.id, this.state, this.currentTerm));
            return new RequestVoteResult (this.id, false, this.currentTerm);
        }
    }

    public synchronized void becomeFollower(int term) {

        cancelScheduledEvents();
        //TODO thread safety
        updateTerm(term);

        if(this.state != RaftServerStateEnum.Follower) {
            this.state = RaftServerStateEnum.Follower;
            this.auditLog.Log(new AuditRecord(
                    AuditRecord.AuditRecordType.BecomeFollower,
                    this.id,
                    this.state,
                    this.currentTerm));
        }

        this.electionTimer = planner.electionDelay(this::becomeCandidate);
    }

    public synchronized void becomeCandidate() {

        //TODO Consider State pattern - doing this in more than one place
        if(this.state != RaftServerStateEnum.Candidate) {
            this.state = RaftServerStateEnum.Candidate;
            this.auditLog.Log(new AuditRecord(
                    AuditRecord.AuditRecordType.BecomeCandidate,
                    this.id,
                    this.state,
                    this.currentTerm));
        }

        resetVotingRecord();
        cancelScheduledEvents();
        currentTerm++;
        auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.StartElection, this.id, this.state, this.currentTerm));

        receiveVote(new RequestVoteResult(this.id,true,this.currentTerm), null);

        this.votedFor = this.id;

        for(ServerProxy s : this.servers) {
            s.RequestVote(new RequestVoteCommand(this.id, this.currentTerm)).whenComplete(this::receiveVote);
        }
    }

    public synchronized void becomeLeader() {
        cancelScheduledEvents();
        this.state = RaftServerStateEnum.Leader;
        this.auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.BecomeLeader, this.id, this.state, this.currentTerm));
        sendHeartbeat();
    }

    private synchronized void sendHeartbeat() {

        if(this.state == RaftServerStateEnum.Leader) {

            var heartBeatCmd = new AppendEntriesCommand(
                    this.id,
                    this.currentTerm
            );

            this.auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.SendHeartbeat, this.id, this.state,this.currentTerm ));

            for(ServerProxy s : this.servers) {
                s.AppendEntries(heartBeatCmd).whenComplete(this::receiveHeartBeatResponse);
            }

            this.heartbeatTimer = this.planner.heartbeatDelay(()->sendHeartbeat());
        }
    }

    private synchronized void receiveHeartBeatResponse(AppendEntriesResult result, Throwable throwable) {
        if(result == null) {
            //TODO schedule retry
        }
        else {
            becomeFollowerIfTermIsStale(result.getTerm());
        }
    }

    private synchronized void receiveVote(RequestVoteResult result, Throwable throwable) {
        if(result == null) {
            //TODO schedule retry
        }
        else {
            if(!becomeFollowerIfTermIsStale(result.getTerm())) {
                if(this.state == RaftServerStateEnum.Candidate && result.isVoteGranted() && this.currentTerm == result.getTerm()) {

                    this.votesReceived++;
                    this.auditLog.Log(new AuditRecord(AuditRecord.AuditRecordType.RecVote, this.id, this.state, this.currentTerm, "Voter: " + result.getVoterId()));
                    if(this.votesReceived > (this.servers.size() + 1) / 2) {
                        becomeLeader();
                    }
                }
            }
        }
    }

    private boolean becomeFollowerIfTermIsStale(int termToCompare) {

        if(staleTermCheck(termToCompare)) {
            becomeFollower(termToCompare);
            return true;
        }
        return false;
    }

    private boolean staleTermCheck(int termToCompare)
    {
        if(termToCompare > this.currentTerm)
        {
            this.auditLog.Log(new AuditRecord(
                    AuditRecord.AuditRecordType.DetectStaleTerm,
                    this.id,
                    this.state,
                    this.currentTerm));

            return true;
        }
        return false;
    }

    private void updateTerm(int term) {

        guardInvalidTermTransition(currentTerm, term);

        if (term > currentTerm) {
            resetVotingRecord();
        }
        currentTerm = term;
    }

    private void resetVotingRecord() {
        votedFor = null;
        votesReceived = 0;
    }

    private void cancelScheduledEvents() {
        if(this.electionTimer != null) {
            this.electionTimer.close();
            this.electionTimer = null;
        }

        if(this.heartbeatTimer != null) {
            this.heartbeatTimer.close();
            this.heartbeatTimer = null;
        }
    }

    public static void guardInvalidTermTransition(int currentTerm, int targetTerm) {
        if(targetTerm < currentTerm) {
            String desc = "Requested to become a Follower with term " + targetTerm + " when term is " + currentTerm;
            InvalidTermTransitionException ex = new InvalidTermTransitionException(desc);
            logger.error("GuardInvalidTermTransition", ex);
            throw ex;
        }
    }
}
