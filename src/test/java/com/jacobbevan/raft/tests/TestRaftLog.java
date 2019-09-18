package com.jacobbevan.raft.tests;

import com.jacobbevan.raft.log.RaftLog;
import com.jacobbevan.raft.mocks.SumIntState;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestRaftLog {

    @Test
    public void initial_index_is_zero() {
        var log = new RaftLog<Integer>("a");
        assertThat(log.getNextIndex(),equalTo(0));
    }

    @Test
    public void first_entry_has_no_previous_entry_term_or_index() {
        var log = new RaftLog<Integer>("a");
        var cmd = log.addEntry(0, 99);
        assertThat(cmd.hasPrevLogIndex(), equalTo(false));
        assertThat(cmd.hasPrevLogTerm(), equalTo(false));
        assertThat(cmd.getPrevLogIndex(), equalTo(-1));
        assertThat(cmd.getPrevLogTerm(), equalTo(-1));
    }

    @Test
    public void second_entry_references_first_entry_as_previous() {
        var log = new RaftLog<Integer>("a");
        int nextIndex = log.getNextIndex();
        var cmd1 = log.addEntry( 0, 99);
        var cmd2 = log.addEntry( 88, 99);
        assertThat(cmd2.hasPrevLogIndex(), equalTo(true));
        assertThat(cmd2.hasPrevLogTerm(), equalTo(true));
        assertThat(cmd2.getPrevLogIndex(), equalTo(nextIndex));
        assertThat(cmd2.getPrevLogTerm(), equalTo(cmd1.getTerm()));
        assertThat(log.getNextIndex(), equalTo(2));
    }

    @Test
    public void append_entries_command_correctly_reflects_leader_information() {
        var log = new RaftLog<Integer>("a");
        var cmd = log.addEntry(0, 99);
        assertThat(cmd.getLeaderId(), equalTo("a"));
    }

    @Test
    public void two_entries_in_same_term_from_one_log_can_be_applied_to_another() {
        var logA = new RaftLog<Integer>("a");
        var logB = new RaftLog<Integer>("b");

        var cmd1 = logA.addEntry(0, 99);
        var cmd2 = logA.addEntry(0, 100);

        assertThat(logB.tryApplyEntry(cmd1), equalTo(true));
        assertThat(logB.tryApplyEntry(cmd2), equalTo(true));
    }

    @Test
    public void two_entries_with_different_terms_from_one_log_can_be_applied_to_another() {
        var logA = new RaftLog<Integer>("a");
        var logB = new RaftLog<Integer>("b");

        var cmd1 = logA.addEntry(0, 99);
        var cmd2 = logA.addEntry(1, 100);

        assertThat(logB.tryApplyEntry(cmd1), equalTo(true));
        assertThat(logB.tryApplyEntry(cmd2), equalTo(true));
    }

    @Test
    public void entry_rejected_if_marked_as_first_and_receiver_already_has_an_entry() {
        var logA = new RaftLog<Integer>("a");
        var logB = new RaftLog<Integer>("b");

        var cmd1 = logA.addEntry(0, 99);
        logB.addEntry(1, 100);

        assertThat(logB.tryApplyEntry(cmd1), equalTo(false));
    }

    @Test
    public void gap_in_record_leads_to_rejection() {
        var logA = new RaftLog<Integer>("a");
        var logB = new RaftLog<Integer>("b");

        var cmd1 = logA.addEntry(0, 99);
        var cmd2 = logA.addEntry(1, 100);
        var cmd3 = logA.addEntry(1, 100);


        assertThat(logB.tryApplyEntry(cmd1), equalTo(true));
        assertThat(logB.tryApplyEntry(cmd3), equalTo(false));
    }

    @Test
    public void duplicate_delivery_leads_to_rejection() {
        var logA = new RaftLog<Integer>("a");
        var logB = new RaftLog<Integer>("b");

        var cmd1 = logA.addEntry(0, 99);
        var cmd2 = logA.addEntry(1, 100);


        assertThat(logB.tryApplyEntry(cmd1), equalTo(true));
        assertThat(logB.tryApplyEntry(cmd2), equalTo(true));
        assertThat(logB.tryApplyEntry(cmd2), equalTo(false));
    }


    @Test
    public void verify_log_truncation() {

        var logA = new RaftLog<Integer>("a");
        var logC = new RaftLog<Integer>("c");

        logC.tryApplyEntry(logA.addEntry(0, 1));
        logC.tryApplyEntry(logA.addEntry(0, 2));
        logC.tryApplyEntry(logA.addEntry(0, 3));
        logC.tryApplyEntry(logA.addEntry(0, 4));
        logC.tryApplyEntry(logA.addEntry(0, 5));

        assertThat(logC.getNextIndex(), equalTo(5));

        var logB = new RaftLog<Integer>("b");
        logB.addEntry(0, 99);
        logB.addEntry(0, 100);

        assertThat(logC.tryApplyEntry(logB.addEntry(0, 100)), equalTo(false));
        assertThat(logC.getNextIndex(), equalTo(1));

    }


    @Test
    public void match_state_built_from_log_and_replicated_log() {

        var stateA = new SumIntState();
        var stateB = new SumIntState();

        var logA = new RaftLog<Integer>("a");
        var logB = new RaftLog<Integer>("b");

        var cmd1 = logA.addEntry(0, 9);
        var cmd2 = logA.addEntry(0, 16);
        var cmd3 = logA.addEntry(0, 25);
        var cmd4 = logA.addEntry(0, 36);

        stateA.apply(cmd1.getLogEntry().getCommand());
        stateA.apply(cmd2.getLogEntry().getCommand());
        stateA.apply(cmd3.getLogEntry().getCommand());
        stateA.apply(cmd4.getLogEntry().getCommand());

        logB.tryApplyEntry(cmd1);
        logB.tryApplyEntry(cmd2);

        assertThat(logB.synchroniseCommits(stateB,1), equalTo(2));
        assertThat(stateB.getTotal(), equalTo(25));

        logB.tryApplyEntry(cmd3);
        logB.tryApplyEntry(cmd4);

        assertThat(logB.synchroniseCommits(stateB,2), equalTo(1));
        assertThat(stateB.getTotal(), equalTo(50));

        assertThat(logB.synchroniseCommits(stateB,3), equalTo(1));
        assertThat(stateB.getTotal(), equalTo(86));


        assertThat(stateB.getTotal(), equalTo(stateA.getTotal()));

    }



}
