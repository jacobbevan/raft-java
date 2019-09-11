package com.jacobbevan.raft.servers;

public class Peer<C> {

    private ServerProxy<C> proxy;
    private int nextIndex;
    private int matchIndex;

    public ServerProxy<C> getProxy() {
        return proxy;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    public Peer(ServerProxy<C> proxy) {
        this.proxy = proxy;
        this.nextIndex = 0;
        this.matchIndex = 0;
    }
}
