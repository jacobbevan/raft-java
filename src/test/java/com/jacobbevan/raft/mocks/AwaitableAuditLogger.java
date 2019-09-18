package com.jacobbevan.raft.mocks;

import com.jacobbevan.raft.audit.AuditLogger;
import com.jacobbevan.raft.audit.AuditRecord;
import com.jacobbevan.raft.servers.RaftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface AwaitableAuditLogger extends AuditLogger {

    void await() throws InterruptedException;
    void reset();

    static AwaitableAuditLogger oneLeaderAllOthersFollow(AuditLogger toDecorate) {


        var agg = maintainDictionaryOfRolesByServer();

        Predicate<Map<String,RaftServer.RaftServerRole>> p = m-> {

            Map<RaftServer.RaftServerRole, Long> counted = m.values().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            return counted.size() == 2 &&
                    counted.containsKey(RaftServer.RaftServerRole.Leader) &&
                    counted.get(RaftServer.RaftServerRole.Leader) == 1 &&
                    counted.containsKey((RaftServer.RaftServerRole.Follower));
        };

        return new MonitoringAuditLogger<>(toDecorate, agg, p, new HashMap<>());
    }

    static BiFunction<Map<String, RaftServer.RaftServerRole>, AuditRecord, Map<String, RaftServer.RaftServerRole>> maintainDictionaryOfRolesByServer() {
        return (s, r) -> {

                s.put(r.getId(), r.getRole());
                return s;
            };
    }

}
