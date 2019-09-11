package com.jacobbevan.raft.servers;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;

import java.util.concurrent.CompletableFuture;

public interface ServerProxy<C> {
    String getId();
    CompletableFuture<AppendEntriesResult> appendEntries(AppendEntriesCommand<C> request);
    CompletableFuture<RequestVoteResult> requestVote(RequestVoteCommand request);
    CompletableFuture<Void> execute(C command);
}
