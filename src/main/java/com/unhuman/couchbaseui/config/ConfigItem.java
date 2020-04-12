package com.unhuman.couchbaseui.config;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnore;

public class ConfigItem {
    @JsonIgnore
    private boolean dirty;

    protected ConfigItem() {
        dirty = false;
    }

    protected void setDirty() {
        dirty = true;
    }

    protected boolean getDirty() {
        return dirty;
    }
}
