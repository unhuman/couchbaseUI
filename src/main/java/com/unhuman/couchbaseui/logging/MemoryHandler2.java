package com.unhuman.couchbaseui.logging;

import com.unhuman.couchbaseui.config.CouchbaseUIConfig;

import java.lang.module.Configuration;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MemoryHandler2 extends Handler {
    private static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
    private List<String> messages;
    private volatile CouchbaseUIConfig configuration;

    /**
     * Memory Handler for logs
     */
    public MemoryHandler2() {
        super();
        configuration = null;
        this.messages = new ArrayList<>(1000);
    }

    public void setConfig(CouchbaseUIConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public void publish(LogRecord record) {
        synchronized (messages) {
            messages.add(LOG_DATE_FORMAT.format(new Date()) + " - " + record.getLevel() + ": " + record.getMessage());
            if (configuration != null && messages.size() > configuration.getLogHistorySize()) {
                this.messages.subList(0, messages.size() - configuration.getLogHistorySize()).clear();
            }
        }
    }

    @Override
    public void flush() {
        messages.clear();
    }

    @Override
    public void close() {
    }

    public List<String> getMessages() {
        synchronized (messages) {
            return Collections.unmodifiableList(new ArrayList<>(messages));
        }
    }
}
