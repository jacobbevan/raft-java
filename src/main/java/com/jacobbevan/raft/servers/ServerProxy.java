package com.jacobbevan.raft.servers;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;

import java.util.concurrent.CompletableFuture;

public interface ServerProxy {
    String getId();
    CompletableFuture<AppendEntriesResult> AppendEntries(AppendEntriesCommand request);
    CompletableFuture<RequestVoteResult> RequestVote(RequestVoteCommand request);
}
