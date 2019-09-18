package com.jacobbevan.raft.log;

import com.jacobbevan.raft.messages.AppendEntriesCommand;

import java.util.ArrayList;
import java.util.List;

public class RaftLog<C> {

    private final String serverId;
    private final List<LogEntry<C>> logEntries = new ArrayList<>();
    private int committedIndex = -1;

    public RaftLog(String serverId) {
        this.serverId = serverId;
    }

    /** Adds a new entry to the log and returns the {@link LogEntry} wrapped in an {@link AppendEntriesCommand} */
    public AppendEntriesCommand<C> addEntry(int term, C command) {

        int prevLogIndex = logEntries.size() == 0 ? -1 : logEntries.size() - 1;
        int prevLogTerm = prevLogIndex == -1 ? -1 : logEntries.get(prevLogIndex).getTerm();

        var logEntry = new LogEntry(command, term, logEntries.size());
        logEntries.add(logEntry);

        return new AppendEntriesCommand<>(serverId,term, committedIndex, logEntry,  prevLogIndex, prevLogTerm);
    }



    /** Attempts to apply the log entry wrapped in an {@link AppendEntriesCommand} to the log. The entry may be rejected if inconsistencies are detected */
    public boolean tryApplyEntry(AppendEntriesCommand<C> appendCmd) {

        if(appendCmd.hasPrevLogIndex()) {

            int prevIndex = appendCmd.getPrevLogIndex();
            int prevTerm = appendCmd.getPrevLogTerm();

            boolean rejectAndTruncate = false;

            if((prevIndex != logEntries.size() - 1) || logEntries.get(prevIndex).getTerm() != prevTerm) {
                //conflict - delete this entry and all that follow it
                for(int i = logEntries.size() - 1; i >= prevIndex; i--) {
                    logEntries.remove(i);
                }
                return false;
            }
        }
        else {
            //cover case where there are no entries - reject if we have at least one entry and the sender thinks this is the 1st
            if(logEntries.size() != 0) {
                return false;
            }
        }

        logEntries.add(appendCmd.getLogEntry());
        return true;
    }

    /** Applies log entries sequentially until the log commit index matches that of the cluster leader **/
    public int synchroniseCommits(State<C> state, int leaderCommit) {

        if(leaderCommit > committedIndex) {
            int committed = 0;

            for(int i = committedIndex + 1; i <= Math.min(leaderCommit, logEntries.size()-1); i++) {
                state.apply(logEntries.get(i).getCommand());
                committed++;
                committedIndex = i;
            }
            return committed;
        }

        return 0;
    }

    //** Report the next index
    public int getNextIndex() {
        return logEntries.size();
    }

    public int getCommittedIndex() {
        return committedIndex;
    }
}
