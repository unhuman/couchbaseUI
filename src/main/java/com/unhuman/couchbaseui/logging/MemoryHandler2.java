package com.unhuman.couchbaseui.logging;

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
    List<String> messages = Collections.synchronizedList(new ArrayList<>());

    /**
     * Memory Handler for logs
     */
    public MemoryHandler2() {
        super();
    }

    @Override
    public void publish(LogRecord record) {
        messages.add(LOG_DATE_FORMAT.format(new Date()) + ": " + record.getMessage());
    }

    @Override
    public void flush() {
        messages.clear();
    }

    @Override
    public void close() {
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
