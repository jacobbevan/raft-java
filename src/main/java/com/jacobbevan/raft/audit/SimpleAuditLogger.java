package com.jacobbevan.raft.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuditLogger implements AuditLogger {
    private final Logger logger = LoggerFactory.getLogger(SimpleAuditLogger.class);
    private final List<AuditRecord> records = new ArrayList();
    private Object lock = new Object();

    @Override
    public void Log(AuditRecord record) {

        synchronized (lock) {
            records.add((record));
            logger.info(record.toString());
        }
    }


}
