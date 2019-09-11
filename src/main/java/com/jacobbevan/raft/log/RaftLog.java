package com.jacobbevan.raft.log;

import com.jacobbevan.raft.messages.AppendEntriesCommand;
import com.jacobbevan.raft.servers.Planner;

import java.util.ArrayList;
import java.util.List;

public class RaftLog<C> {

    private List<LogEntry<C>> logEntries = new ArrayList<>();
    private int commitedIndex;

    public AppendEntriesCommand<C> addEntry(String serverId, int term, C command) {

        int prevLogIndex = logEntries.size() == 0 ? -1 : logEntries.size();
        int prevLogTerm = prevLogIndex == -1 ? -1 : logEntries.get(prevLogIndex).getTerm();

        var logEntry = new LogEntry(command, term, logEntries.size());
        logEntries.add(logEntry);

        return new AppendEntriesCommand<>(serverId,term, commitedIndex, logEntry,  prevLogIndex, prevLogTerm);
    }

    public boolean tryApplyEntry(AppendEntriesCommand<C> appendCmd) {

        int prevIndex = appendCmd.getPrevLogIndex();
        int prevTerm = appendCmd.getPrevLogTerm();

        if(prevIndex != -1) {
            if(prevIndex >= logEntries.size()) {
                //gap in the record
                return false;
            }

            if(logEntries.get(prevIndex).getTerm() != prevTerm) {
                //conflict - delete this entry and all that follow it
                for(int i = logEntries.size() - 1; i >=prevIndex; i--) {
                    logEntries.remove(i);
                }
                return false;
            }
        }
        else {
            //cover case where there are no entries - reject if we have at least one entry and the sender thinks this is the 1st
            if(logEntries.size() == 0) {
                return false;
            }
        }

        logEntries.add(appendCmd.getLogEntry());



        return true;

    }

    public int synchroniseCommits(State<C> state, int leaderCommit) {

        if(leaderCommit > commitedIndex) {
            int committed = 0;

            for(int i = commitedIndex; i < Math.min(leaderCommit, logEntries.size()); i++) {
                state.apply(logEntries.get(i).getCommand());
                committed++;
                commitedIndex = i;
            }
            return committed;
        }

        return 0;
    }

    public int getNextIndex() {
        return logEntries.size();
    }

    public int getCommitedIndex() {
        return commitedIndex;
    }
}
