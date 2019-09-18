package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.audit.SimpleAuditLogger;
import com.jacobbevan.raft.log.State;
import com.jacobbevan.raft.mocks.AwaitableAuditLogger;
import com.jacobbevan.raft.mocks.ReliableServerProxy;
import com.jacobbevan.raft.mocks.SumIntState;
import com.jacobbevan.raft.servers.RaftServer;
import com.jacobbevan.raft.servers.SchedulePlanner;
import com.jacobbevan.raft.servers.Server;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestLogReplication {

    /*
    List<Server<Integer>> servers;

    @Before
    public  void create_test_cluster() throws InterruptedException {


        var innerLog = new SimpleAuditLogger();
        var log = AwaitableAuditLogger.oneLeaderAllOthersFollow(innerLog);
        var planner = new SchedulePlanner(500,400,200,50);

        int latency = 100;
        var ids = Arrays.asList("a","b","c","d","e");

        State<Integer> state =  new SumIntState();
        servers = ids.stream().map(id->new RaftServer<>(id, state, planner, log)).collect(Collectors.toList());
        var proxies = servers.stream().map(s->new ReliableServerProxy<Integer>(s, latency)).collect(Collectors.toList());

        for (Server server : servers) {
            server.initialise(proxies);
        }

        //await leader election
        log.await();
    }


    private Server<Integer> findLeader() {
        for(var s : servers) {
            if(s.getRole() == RaftServer.RaftServerRole.Leader) {
                return s;
            }
        }
        throw new RuntimeException("Leader not found");
    }

    @Test
    public void abc() throws IOException, InterruptedException {

        var leader = findLeader();

        leader.execute(101);
        Thread.sleep(300000);
    }
    */

}
