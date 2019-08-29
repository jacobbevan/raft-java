package com.jacobbevan.raft.audit;

public interface AuditLogger {
    void Log(AuditRecord record);
}
