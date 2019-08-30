package com.jacobbevan.raft.servers;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.messages.AppendEntriesResult;
import com.jacobbevan.raft.messages.RequestVoteCommand;
import com.jacobbevan.raft.messages.RequestVoteResult;

import java.util.Collection;
import java.util.Collections;

public interface Server {

    String getId();
    int getCurrentTerm();
    RaftServer.RaftServerStateEnum getState();
    AppendEntriesResult appendEntries(AppendEntriesCommand request);
    RequestVoteResult requestVote(RequestVoteCommand request);
    void initialise(Collection<ServerProxy> servers);

}
