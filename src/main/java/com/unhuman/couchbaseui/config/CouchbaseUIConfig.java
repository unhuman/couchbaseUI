package com.unhuman.couchbaseui.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;

import java.util.*;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CouchbaseUIConfig {
    @JsonIgnore
    private String secret;

    private Map<String, ClusterConfig> servers;

    private N1QLQueryRefreshHandling n1QLQueryRefreshHandling = N1QLQueryRefreshHandling.InPlace;

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

    public boolean hasSecret() {
        return secret != null;
    }

    @JsonIgnore
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = (secret != null && secret.trim().length() > 0) ? secret.trim() : null;
    }


    @JsonIgnore
    public List<String> getServerHostnames() {
        List<String> hostnames = new ArrayList<>(servers.keySet());
        Collections.sort(hostnames);
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
            userConfig.upsertQuery(n1QLQueryRefreshHandling, query);

            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.upsertConfigServer(clusterConnection.getUser(), userConfig);

            this.servers.put(clusterConnection.getHost(), clusterConfig);
        } else {
            ClusterConfig clusterConfig = this.servers.get(clusterConnection.getHost());
            UserConfig userConfig = clusterConfig.getUserConfig(clusterConnection.getUser());
            userConfig.upsertQuery(n1QLQueryRefreshHandling, query);
        }
    }

    public N1QLQueryRefreshHandling getN1QLQueryRefreshHandling() {
        return n1QLQueryRefreshHandling;
    }

    public void setN1QLQueryRefreshHandling(N1QLQueryRefreshHandling n1QLQueryRefreshHandling) {
        this.n1QLQueryRefreshHandling = n1QLQueryRefreshHandling;
    }
}


