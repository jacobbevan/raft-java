package com.jacobbevan.raft.servers;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public interface Server<C> {

    String getId();
    int getCurrentTerm();
    RaftServer.RaftServerStateEnum getState();
    AppendEntriesResult appendEntries(AppendEntriesCommand<C> request);
    RequestVoteResult requestVote(RequestVoteCommand request);
    //TODO consider splitting out into separate interface
    Void execute(C command) throws IOException;
    //TODO support for discovery should enable removal
    void initialise(Collection<ServerProxy> servers);
}
