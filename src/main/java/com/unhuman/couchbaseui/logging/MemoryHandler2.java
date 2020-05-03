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
        // handle log levels
        if (record.getLoggerName().startsWith("com.couchbase")
                && record.getLevel().intValue() < configuration.getCouchbaseClientLogLevel().intValue()) {
            return;
        }
        if (record.getLoggerName().startsWith("com.unhuman")
                && record.getLevel().intValue() < configuration.getCouchbaseUILogLevel().intValue()) {
            return;
        }

        StringBuilder messageBuilder = new StringBuilder(record.getMessage().length() + 256);
        messageBuilder.append(LOG_DATE_FORMAT.format(new Date()));
        messageBuilder.append(" - ");
        messageBuilder.append(record.getLevel());
        messageBuilder.append(": ");
        if (!record.getMessage().contains(record.getLoggerName())) {
            messageBuilder.append(record.getLoggerName());
            messageBuilder.append(" - ");
        }
        messageBuilder.append(record.getMessage());
        String message = messageBuilder.toString();

        synchronized (messages) {
            messages.add(message);
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
