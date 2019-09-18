package com.jacobbevan.raft.mocks;

import com.jacobbevan.raft.log.State;

public class SumIntState implements State<Integer> {

    private int total = 0;

    @Override
    public void apply(Integer command) {

        total = total + (int) command;
    }

    public int getTotal() {
        return total;
    }
}
