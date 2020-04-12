package com.unhuman.couchbaseui.config;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConfigTester {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void TestConfigSaveLoad() throws JsonProcessingException {
        CouchbaseUIConfig couchbaseUIConfig = CouchbaseUIConfig.CreateConfigFile(null);

        ClusterConnection clusterConnection =
                new ClusterConnection("hostname", "user", "password");
        BucketCollection bucketCollection = new BucketCollection("item", "");
        couchbaseUIConfig.upsertServer(clusterConnection, bucketCollection);

        String serialized = OBJECT_MAPPER.writeValueAsString(couchbaseUIConfig);
        assertNotEquals("{}", serialized);
        assertFalse(serialized.contains("dirty"));
    }
}
