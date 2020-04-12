package com.unhuman.couchbaseui.config;

import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class ConfigFileManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final File getConfigFile() {
        // TODO: Would be nice to use AppData on Windows
        return new File(System.getProperty("user.home") + "/.couchbaseUI.config");
    }

    public static CouchbaseUIConfig CreateEmptyConfig() {
        return CouchbaseUIConfig.CreateConfigFile(null);
    }

    public static CouchbaseUIConfig LoadConfig() throws Exception {
        File file = getConfigFile();

        if (file.exists()) {
            return OBJECT_MAPPER.readValue(file, CouchbaseUIConfig.class);
        } else {
            // file didn't exist, just return nothing
            return CreateEmptyConfig();
        }
    }

    public static void SaveConfig(CouchbaseUIConfig config) throws Exception {
        File file = getConfigFile();
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, config);
    }
}
