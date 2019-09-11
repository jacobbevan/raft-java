package com.jacobbevan.raft.mocks;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;
import com.jacobbevan.raft.servers.Server;
import com.jacobbevan.raft.servers.ServerProxy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReliableServerProxy<C> implements ServerProxy<C> {

    private Server server;
    private int latency;

    private final ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor();

    public ReliableServerProxy(Server server, int latency) {
        this.server = server;
        this.latency = latency;
    }

    @Override
    public String getId() {
        return server.getId();
    }

    @Override
    public CompletableFuture<AppendEntriesResult> appendEntries(AppendEntriesCommand<C> request) {
        return CompletableFuture.supplyAsync(()->server.appendEntries(request), CompletableFuture.delayedExecutor(latency, TimeUnit.MILLISECONDS, timerService));
    }

    @Override
    public CompletableFuture<RequestVoteResult> requestVote(RequestVoteCommand request) {
        return CompletableFuture.supplyAsync(()->server.requestVote(request), CompletableFuture.delayedExecutor(latency, TimeUnit.MILLISECONDS, timerService));
    }

    @Override
    public CompletableFuture<Void> execute(C command) {
        return CompletableFuture.supplyAsync(()-> {
            try {
                return server.execute(command);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
           }
        }, CompletableFuture.delayedExecutor(latency, TimeUnit.MILLISECONDS, timerService));
    }
}
