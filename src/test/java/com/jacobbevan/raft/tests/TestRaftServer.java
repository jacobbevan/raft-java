package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;
import com.jacobbevan.raft.servers.*;
import com.jacobbevan.raft.audit.AuditLogger;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

public class TestRaftServer {


    private AuditLogger log;
    private Planner planner;

    @Before
    public void setup() {
        log = mock(AuditLogger.class);
        planner = mock(Planner.class);
    }

    @Test
    public void server_starts_as_follower() throws InvalidTermTransitionException {
        var server = new RaftServer("a", planner, log);
        assertThat(server.getId(), is("a"));
        assertThat(server.getState(), is(RaftServer.RaftServerStateEnum.Follower));
    }


    @Test
    public void follower_plans_to_become_candidate() throws InvalidTermTransitionException {

        var electionTimer = mock(SafeAutoCloseable.class);
        when(planner.electionDelay(ArgumentMatchers.any())).thenReturn(electionTimer);
        var server = new RaftServer("a", planner, log);

        verify(planner).electionDelay(ArgumentMatchers.any());
        verify(electionTimer, never()).close();

    }

    @Test
    public void observing_leader_suppresses_election_timeout_and_schedules_new_one() throws InvalidTermTransitionException {

        var electionTimer = mock(SafeAutoCloseable.class);
        var electionTimer2 = mock(SafeAutoCloseable.class);

        when(planner.electionDelay(ArgumentMatchers.any())).thenReturn(electionTimer).thenReturn((electionTimer2));
        var server = new RaftServer("a", planner, log);
        server.appendEntries(new AppendEntriesCommand("a", 2));
        assertThat(server.getState(), is(RaftServer.RaftServerStateEnum.Follower));
        assertThat(server.getCurrentTerm(), is(2));

        verify(planner,times(2)).electionDelay(ArgumentMatchers.any());
        verify(electionTimer).close();
        verify(electionTimer2, never()).close();

    }

    @Test
    public void become_candidate_results_in() throws InvalidTermTransitionException {

        var voteRequest = new RequestVoteCommand("a", 1);

        var otherServer = mock(ServerProxy.class);
        var electionTimer = mock(SafeAutoCloseable.class);
        var electionTimer2 = mock(SafeAutoCloseable.class);
        var cf = mock(CompletableFuture.class);

        when(planner.electionDelay(ArgumentMatchers.any())).thenReturn(electionTimer).thenReturn((electionTimer2));
        when(otherServer.RequestVote(ArgumentMatchers.eq(voteRequest))).thenReturn(cf);

        var server = new RaftServer("a", planner, log);
        server.initialise(Arrays.asList(otherServer));
        verify(planner,times(1)).electionDelay(ArgumentMatchers.any());
        server.becomeCandidate();
        assertThat(server.getState(), is(RaftServer.RaftServerStateEnum.Candidate));

    }

    @Test(expected = InvalidTermTransitionException.class)
    public void reducing_term_results_in_exception() throws InvalidTermTransitionException {

        RaftServer server = new RaftServer("a", planner,log);
        server.becomeFollower(1);
        server.becomeFollower(0);
    }




}
