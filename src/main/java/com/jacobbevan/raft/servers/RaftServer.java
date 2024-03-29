package com.jacobbevan.raft.servers;

import com.jacobbevan.raft.audit.AuditLogger;
import com.jacobbevan.raft.audit.AuditRecord;
import com.jacobbevan.raft.log.RaftLog;
import com.jacobbevan.raft.log.State;
import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class RaftServer<C> implements Server<C> {


    public enum RaftServerRole {
        Leader,
        Follower,
        Candidate
    }

    private static Logger logger = LoggerFactory.getLogger(RaftServer.class);
    private static final int INITIAL_TERM = 0;

    private final String id;
    private final Planner planner;
    private final AuditLogger auditLog;
    //TODO injection for testing
    private final RaftLog<C> log;
    private final State<C> state;
    private RaftServerRole role;
    private int currentTerm;
    private String votedFor;
    private String leaderId;
    private int votesReceived = 0;
    private Map<String, Peer<C>> servers = new HashMap<>();
    private SafeAutoCloseable electionTimer;
    private SafeAutoCloseable heartbeatTimer;

    private final Object lock = new Object();

    private static void guardInvalidTermTransition(int currentTerm, int targetTerm) {
        if(targetTerm < currentTerm) {
            String desc = "Requested to become a Follower with term " + targetTerm + " when term is " + currentTerm;
            var ex = new InvalidTermTransitionException(desc);
            logger.error("GuardInvalidTermTransition", ex);
            throw ex;
        }
    }

    public RaftServer(final String id, final State<C> initialState, Planner planner, AuditLogger auditLog) {
        this.id = id;
        this.log = new RaftLog<>(id);
        this.planner = planner;
        this.auditLog = auditLog;
        this.state = initialState;
        becomeFollower(INITIAL_TERM);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getCurrentTerm() {
        synchronized (lock) {
            return currentTerm;

        }
    }

    @Override
    public RaftServerRole getRole() {
        synchronized (lock) {
            return role;
        }
    }


    @Override
    public void initialise(Collection<ServerProxy> servers) {
        synchronized (lock) {
            //TODO replace with server discover
            for (ServerProxy server : servers) {
                if (server.getId() != id) {
                    this.servers.put(server.getId(), new Peer(server));
                }
            }
        }
    }

    @Override
    public AppendEntriesResult appendEntries(AppendEntriesCommand<C> request) {

        synchronized (lock) {
            auditLog(AuditRecord.AuditRecordType.RecAppendEntries);

            leaderId = request.getLeaderId();

            if (currentTerm > request.getTerm()) {
                auditLog(AuditRecord.AuditRecordType.RejectAppendEntries);
                return new AppendEntriesResult(false, currentTerm);
            }

            becomeFollower(request.getTerm());

            boolean success = true;

            if (request.hasLogEntry()) {
                success = log.tryApplyEntry(request);
                auditLog(success ? AuditRecord.AuditRecordType.AcceptLogUpdate : AuditRecord.AuditRecordType.RejectLogUpdate);
            }

            var commits = log.synchroniseCommits(state, request.getMaxCommit());
            if (commits > 0) {
                auditLog(AuditRecord.AuditRecordType.SyncroniseLogs, "CommittedEntries: " + commits);
            }

            return new AppendEntriesResult(success, currentTerm);
        }
    }

    @Override
    public RequestVoteResult requestVote(RequestVoteCommand request) {

        synchronized (lock) {
            auditLog(AuditRecord.AuditRecordType.RecVoteRequest, "Requestor: " + request.getCandidateId());

            becomeFollowerIfTermIsStale(request.getCurrentTerm());

            if ((votedFor == request.getCandidateId() || votedFor == null) && request.getCurrentTerm() >= currentTerm) {
                auditLog(AuditRecord.AuditRecordType.AcceptVoteRequest);

                votedFor = request.getCandidateId();
                return new RequestVoteResult(id, true, currentTerm);
            } else {
                //TODO do we need to differentiate between rejects because we already voted for someone else, versus rejects because the requestors term is stale?
                auditLog(AuditRecord.AuditRecordType.RejectVoteRequest);
                return new RequestVoteResult(id, false, currentTerm);
            }
        }
    }

    @Override
    public Void execute(C command) throws IOException {
        synchronized (lock) {
            //TODO consider State pattern
            if (role == RaftServerRole.Candidate.Leader) {
                sendLogUpdate(command);
            } else {
                if (leaderId != null) {
                    //TODO async completion...?
                    servers.get(leaderId).getProxy().execute(command);
                } else {
                    throw new IOException("Cannot service request, leader is not known");
                }
            }
            return null;
        }
    }

    public void becomeFollower(int term) {

        synchronized (lock) {
            cancelScheduledEvents();
            //TODO thread safety
            updateTerm(term);

            if (role != RaftServerRole.Follower) {
                role = RaftServerRole.Follower;
                auditLog(AuditRecord.AuditRecordType.BecomeFollower);
            }

            electionTimer = planner.electionDelay(this::becomeCandidate);
        }
    }

    public void becomeCandidate() {

        synchronized (lock) {
            //TODO Consider State pattern - doing this in more than one place
            if (role != RaftServerRole.Candidate) {
                role = RaftServerRole.Candidate;
                auditLog(AuditRecord.AuditRecordType.BecomeCandidate);
            }

            resetVotingRecord();
            cancelScheduledEvents();
            leaderId = null;
            currentTerm++;
            auditLog(AuditRecord.AuditRecordType.StartElection);

            receiveVote(new RequestVoteResult(id, true, currentTerm), null);

            votedFor = id;

            for (Peer<C> s : servers.values()) {
                s.getProxy().requestVote(new RequestVoteCommand(id, currentTerm)).whenComplete(this::receiveVote);
            }
        }
    }

    public void becomeLeader() {

        synchronized (lock) {
            cancelScheduledEvents();
            role = RaftServerRole.Leader;
            auditLog(AuditRecord.AuditRecordType.BecomeLeader);

            for (var peer : servers.values()) {
                peer.setNextIndex(log.getNextIndex());
            }

            sendHeartbeat();
        }
    }

    private void sendLogUpdate(C command) {

        synchronized (lock) {
            //TODO retry
            var appendCmd = log.addEntry(currentTerm, command);

            auditLog(AuditRecord.AuditRecordType.SendLogUpdate);

            for (Peer<C> s : servers.values()) {
                s.getProxy().appendEntries(appendCmd).whenComplete(this::receiveHeartBeatResponse);
            }
        }
    }

    private void receiveAppendEntriesResponse(AppendEntriesResult result, Throwable throwable) {
        synchronized (lock) {
            if (result == null) {
                //TODO schedule retry
            } else {
                if (!becomeFollowerIfTermIsStale(result.getTerm())) {
                    if (!result.isSuccess()) {

                    }
                }
            }
        }
    }

    private void sendHeartbeat() {

        synchronized (lock) {
            if (role == RaftServerRole.Leader) {

                var heartBeatCmd = new AppendEntriesCommand<C>(
                        id,
                        currentTerm,
                        log.getCommittedIndex()
                );

                auditLog(AuditRecord.AuditRecordType.SendHeartbeat);

                for (Peer<C> s : servers.values()) {
                    s.getProxy().appendEntries(heartBeatCmd).whenComplete(this::receiveHeartBeatResponse);
                }

                heartbeatTimer = planner.heartbeatDelay(() -> sendHeartbeat());
            }
        }
    }


    private void receiveHeartBeatResponse(AppendEntriesResult result, Throwable throwable) {
        synchronized (lock) {
            if (result == null) {
                //TODO schedule retry
            } else {
                becomeFollowerIfTermIsStale(result.getTerm());
            }
        }
    }

    private void receiveVote(RequestVoteResult result, Throwable throwable) {

        synchronized (lock) {
            if (result == null) {
                //TODO schedule retry
            } else {
                if (!becomeFollowerIfTermIsStale(result.getTerm())) {
                    if (role == RaftServerRole.Candidate && result.isVoteGranted() && currentTerm == result.getTerm()) {

                        votesReceived++;
                        auditLog(AuditRecord.AuditRecordType.RecVote, "Voter: " + result.getVoterId());
                        if (votesReceived > (servers.size() + 1) / 2) {
                            becomeLeader();
                        }
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
        if(termToCompare > currentTerm)
        {
            auditLog(AuditRecord.AuditRecordType.DetectStaleTerm);
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
        if(electionTimer != null) {
            electionTimer.close();
            electionTimer = null;
        }

        if(heartbeatTimer != null) {
            heartbeatTimer.close();
            heartbeatTimer = null;
        }
    }


    private void auditLog(AuditRecord.AuditRecordType type, String extraInfo) {
        auditLog.Log(new AuditRecord(type, id, role, currentTerm, extraInfo));
    }

    private void auditLog(AuditRecord.AuditRecordType type) {
        auditLog.Log(new AuditRecord(type, id, role, currentTerm));
    }

}
