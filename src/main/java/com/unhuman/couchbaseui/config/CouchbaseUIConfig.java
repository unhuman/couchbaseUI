package com.unhuman.couchbaseui.config;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnore;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CouchbaseUIConfig {
    private Map<String, ClusterConfig> servers;

    private CouchbaseUIConfig() { } // for deserialization

    private CouchbaseUIConfig(Map<String, ClusterConfig> servers) {
        this.servers = new HashMap<>();
        if (servers != null) {
            this.servers.putAll(servers);
        }
    }

    static CouchbaseUIConfig CreateConfigFile(Map<String, ClusterConfig> servers) {
        return new CouchbaseUIConfig(servers);
    }

    @JsonIgnore
    public List<String> getServerHostnames() {
        List<String> hostnames = new ArrayList<>(servers.keySet());
        hostnames.sort(String::compareTo);
        return hostnames;
    }

    public ClusterConfig getClusterConfig(String hostname) {
        return servers.get(hostname);
    }

    /**
     * upserts (add or update) a server
     * @param clusterConnection
     * @param bucketCollection
     * @return
     */
    public void upsertServer(ClusterConnection clusterConnection, BucketCollection bucketCollection) {
        if (!this.servers.containsKey(clusterConnection.getHost())) {
            UserConfig userConfig = new UserConfig();
            userConfig.upsertBucketCollection(bucketCollection.getBucketName(), bucketCollection.getCollectionName());
            userConfig.setPassword(clusterConnection.getPassword());

            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.upsertConfigServer(clusterConnection.getUser(), userConfig);

            this.servers.put(clusterConnection.getHost(), clusterConfig);
        } else {
            ClusterConfig clusterConfig = this.servers.get(clusterConnection.getHost());
            UserConfig userConfig = clusterConfig.getUserConfig(clusterConnection.getUser());
            userConfig.upsertBucketCollection(bucketCollection.getBucketName(), bucketCollection.getCollectionName());
            userConfig.setPassword(clusterConnection.getPassword());
        }
    }

    /**
     * upserts (add or update) a server
     * @param clusterConnection
     * @param query
     * @return
     */
    public void upsertServer(ClusterConnection clusterConnection, String query) {
        if (!this.servers.containsKey(clusterConnection.getHost())) {
            UserConfig userConfig = new UserConfig();
            userConfig.upsertQuery(query);

            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.upsertConfigServer(clusterConnection.getUser(), userConfig);

            this.servers.put(clusterConnection.getHost(), clusterConfig);
        } else {
            ClusterConfig clusterConfig = this.servers.get(clusterConnection.getHost());
            UserConfig userConfig = clusterConfig.getUserConfig(clusterConnection.getUser());
            userConfig.upsertQuery(query);
        }
    }
}


