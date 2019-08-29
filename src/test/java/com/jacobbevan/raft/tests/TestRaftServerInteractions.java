package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.audit.AuditLogger;
import com.jacobbevan.raft.servers.Planner;
import com.jacobbevan.raft.servers.RaftServer;
import com.jacobbevan.raft.servers.Server;
import com.jacobbevan.raft.servers.ServerProxy;
import org.junit.Test;
import testProxies.ReliableProxy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

public class TestRaftServerInteractions {

    @Test
    public void leader_elected_on_reliable_network() {

        var log = mock(AuditLogger.class);
        var planner = mock(Planner.class);

        int latency = 100;
        var ids = Arrays.asList("a","b","c","d","e");

        List<Server> servers = ids.stream().map(id->new RaftServer(id,planner, log)).collect(Collectors.toList());
        List<ServerProxy> proxies = servers.stream().map(s->new ReliableProxy(s, latency)).collect(Collectors.toList());

        for (Server server : servers) {
            server.initialise(proxies);
        }
    }
}
