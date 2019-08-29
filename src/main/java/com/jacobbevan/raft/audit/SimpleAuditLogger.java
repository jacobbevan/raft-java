package com.jacobbevan.raft.audit;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuditLogger implements AuditLogger {
    private List<AuditRecord> records = new ArrayList();

    @Override
    public void Log(AuditRecord record) {
        records.add((record));
    }
}
