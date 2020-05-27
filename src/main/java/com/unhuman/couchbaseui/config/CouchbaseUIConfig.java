package com.unhuman.couchbaseui.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;

import java.util.*;
import java.util.logging.Level;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CouchbaseUIConfig {
    private static final int DEFAULT_LOG_SIZE = 250;
    public static final int MAXIMUM_LOG_SIZE = 10000;

    @JsonIgnore
    private String secret;

    private Integer logHistorySize;

    @JsonIgnore
    private Level couchbaseUILogLevel;

    @JsonIgnore
    private Level couchbaseClientLogLevel;

    private boolean logsWordWrap;

    private Map<String, ClusterConfig> clusters;

    private N1QLQueryRefreshHandling n1QLQueryRefreshHandling = N1QLQueryRefreshHandling.InPlace;

    private CouchbaseUIConfig() { } // for deserialization

    private CouchbaseUIConfig(Map<String, ClusterConfig> clusters) {
        this.clusters = new HashMap<>();
        if (clusters != null) {
            this.clusters.putAll(clusters);
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

    public Integer getLogHistorySize() {
        return (logHistorySize != null) ? logHistorySize : DEFAULT_LOG_SIZE;
    }

    public void setLogHistorySize(Integer logHistorySize) {
        this.logHistorySize = logHistorySize;
    }

    @JsonIgnore
    public Level getCouchbaseUILogLevel() {
        return (couchbaseUILogLevel != null) ? couchbaseUILogLevel : Level.OFF;
    }

    @JsonProperty("couchbaseUILogLevel")
    public String getCouchbaseUILogLevelForConfigFile() {
        return getCouchbaseUILogLevel().getName();
    }

    @JsonIgnore
    public void setCouchbaseUILogLevel(Level couchbaseUILogLevel) {
        this.couchbaseUILogLevel = couchbaseUILogLevel;
    }

    @JsonProperty("couchbaseUILogLevel")
    public void setCouchbaseUILogLevelFromConfigFile(String value) {
        this.couchbaseUILogLevel = Level.parse(value);
    }

    @JsonIgnore
    public Level getCouchbaseClientLogLevel() {
        return (couchbaseClientLogLevel != null) ? couchbaseClientLogLevel : Level.OFF;
    }

    @JsonProperty("couchbaseClientLogLevel")
    public String getCouchbaseClientLogLevelForConfigFile() {
        return getCouchbaseClientLogLevel().getName();
    }

    @JsonIgnore
    public void setCouchbaseClientLogLevel(Level couchbaseClientLogLevel) {
        this.couchbaseClientLogLevel = couchbaseClientLogLevel;
    }

    @JsonProperty("couchbaseClientLogLevel")
    public void setCouchbaseClientLogLevelFromConfigFile(String value) {
        this.couchbaseClientLogLevel = Level.parse(value);
    }

    public boolean isLogsWordWrap() {
        return logsWordWrap;
    }

    public void setLogsWordWrap(boolean logsWordWrap) {
        this.logsWordWrap = logsWordWrap;
    }

    @JsonIgnore
    public List<String> getServerHostnames() {
        List<String> hostnames = new ArrayList<>(clusters.keySet());
        Collections.sort(hostnames);
        return hostnames;
    }

    public ClusterConfig getClusterConfig(String hostname) {
        return clusters.get(hostname);
    }

    /**
     * upserts (add or update) a server
     * @param clusterConnection
     * @param bucketCollection
     * @return
     */
    public void upsertServer(ClusterConnection clusterConnection, BucketCollection bucketCollection) {
        if (!this.clusters.containsKey(clusterConnection.getHost())) {
            UserConfig userConfig = UserConfig.createNewUserConfig();
            userConfig.upsertBucketCollection(bucketCollection.getBucketName(), bucketCollection.getCollectionName());
            userConfig.setPassword(clusterConnection.getPassword());

            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.upsertConfigServer(clusterConnection.getUser(), userConfig);

            this.clusters.put(clusterConnection.getHost(), clusterConfig);
        } else {
            ClusterConfig clusterConfig = this.clusters.get(clusterConnection.getHost());
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
        if (!this.clusters.containsKey(clusterConnection.getHost())) {
            UserConfig userConfig = UserConfig.createNewUserConfig();
            userConfig.upsertQuery(n1QLQueryRefreshHandling, query);

            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.upsertConfigServer(clusterConnection.getUser(), userConfig);

            this.clusters.put(clusterConnection.getHost(), clusterConfig);
        } else {
            ClusterConfig clusterConfig = this.clusters.get(clusterConnection.getHost());
            UserConfig userConfig = clusterConfig.getUserConfig(clusterConnection.getUser());
            userConfig.upsertQuery(n1QLQueryRefreshHandling, query);
        }
    }

    public void removeCluster(String clusterName) {
        clusters.remove(clusterName);
    }

    public N1QLQueryRefreshHandling getN1QLQueryRefreshHandling() {
        return n1QLQueryRefreshHandling;
    }

    public void setN1QLQueryRefreshHandling(N1QLQueryRefreshHandling n1QLQueryRefreshHandling) {
        this.n1QLQueryRefreshHandling = n1QLQueryRefreshHandling;
    }
}


