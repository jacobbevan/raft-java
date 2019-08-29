package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.servers.*;
import com.jacobbevan.raft.audit.AuditLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

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
        Assert.assertEquals("a", server.getId());
        Assert.assertEquals(RaftServer.RaftServerStateEnum.Follower, server.getState());
    }


    @Test
    public void follower_plans_to_become_candidate() throws InvalidTermTransitionException {

        var electionTimer = mock(SafeAutoCloseable.class);
        when(planner.electionDelay(ArgumentMatchers.any())).thenReturn(electionTimer);
        var server = new RaftServer("a", planner, log);

        verify(planner).electionDelay(ArgumentMatchers.any());

    }

    @Test(expected = InvalidTermTransitionException.class)
    public void reducing_term_results_in_exception() throws InvalidTermTransitionException {

        RaftServer server = new RaftServer("a", planner,log);
        server.becomeFollower(1);
        server.becomeFollower(0);
    }




}
