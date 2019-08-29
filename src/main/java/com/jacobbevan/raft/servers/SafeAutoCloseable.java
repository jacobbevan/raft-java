package com.jacobbevan.raft.servers;

public interface SafeAutoCloseable extends AutoCloseable{

    @Override
    void close();
}
