package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.audit.SimpleAuditLogger;
import com.jacobbevan.raft.servers.*;
import org.junit.Ignore;
import org.junit.Test;
import com.jacobbevan.raft.mocks.ReliableServerProxy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

public class TestRaftServerInteractions {

    @Ignore
    @Test
    public void leader_elected_on_reliable_network() throws InterruptedException {

        var log = new SimpleAuditLogger();
        var planner = new SchedulePlanner(500,400,200,50);

        int latency = 100;
        var ids = Arrays.asList("a","b","c","d","e");

        List<Server> servers = ids.stream().map(id->new RaftServer(id,planner, log)).collect(Collectors.toList());
        List<ServerProxy> proxies = servers.stream().map(s->new ReliableServerProxy(s, latency)).collect(Collectors.toList());

        for (Server server : servers) {
            server.initialise(proxies);
        }

        Thread.sleep(30000);

    }

}
