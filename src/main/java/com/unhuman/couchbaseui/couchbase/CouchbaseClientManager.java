package com.unhuman.couchbaseui.couchbase;

import com.couchbase.client.core.env.PasswordAuthenticator;
import com.couchbase.client.core.env.SaslMechanism;
import com.couchbase.client.core.retry.FailFastRetryStrategy;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.unhuman.couchbaseui.AlertsDialog;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConnection;
import com.unhuman.couchbaseui.utils.Utilities;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import java.awt.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.ClusterOptions.clusterOptions;

public class CouchbaseClientManager {
    private static final String CLUSTER_MAP_ENDPOINT = "/pools/nodes";
    public static final String CLUSTER_MAP_NODES = "nodes";
    public static final String CLUSTER_MAP_SERVICES = "services";
    private static final String CLUSTER_MAP_KV_SERVICE = "kv";
    public static final String CLUSTER_MAP_HOSTNAME = "hostname";

    private static final Duration WAIT_UNTIL_READY_DURATION = Duration.ofSeconds(5);

    Map<ClusterConnection, Cluster> connectedClusters;

    public CouchbaseClientManager() {
        this.connectedClusters = new HashMap<>();
    }

    public Collection getBucketCollection(Component parentComponent, ClusterConnection clusterConnection,
                                          BucketCollection bucketCollection)
            throws IOException, JSONException {
        if (Utilities.stringIsNullOrEmpty(bucketCollection.getBucketName())) {
            throw new RuntimeException("Bucket Name must be provided");
        }

        Bucket bucket = getCluster(parentComponent, clusterConnection).bucket(bucketCollection.getBucketName());
        bucket.waitUntilReady(WAIT_UNTIL_READY_DURATION);
        return (bucketCollection.hasCollectionName())
                ? bucket.collection(bucketCollection.getCollectionName())
                : bucket.defaultCollection();
    }

    public Cluster getCluster(Component parentComponent, ClusterConnection clusterConnection)
            throws IOException, JSONException {
        // Check the local cache
        if (connectedClusters.containsKey(clusterConnection)) {
            return connectedClusters.get(clusterConnection);
        }

        JSONObject responseContents = getClusterInfo(clusterConnection);

        if (AlertsDialog.display(parentComponent, responseContents.getJSONArray("alerts"))) {
            Resty restHandler = new Resty();
            JSONResource clearResource = restHandler.json(
                    "http://" + clusterConnection.getHost() + ":8091" + responseContents.get("alertsSilenceURL"));
        }

        List<String> kvNodes = getKVNodes(responseContents);

        // connect to the cluster (right now, it just picks first item out of cluster map)
        ClusterEnvironment environment = ClusterEnvironment.builder()
                .retryStrategy(FailFastRetryStrategy.INSTANCE)
                .build();

        PasswordAuthenticator.Builder passwordAuthenticatorBuilder = PasswordAuthenticator.builder()
                .username(clusterConnection.getUser())
                .password(clusterConnection.getPassword());
        // Workaround for: https://issues.couchbase.com/browse/JVMCBC-528
        if ("Administrator".equals(clusterConnection.getUser())) {
            passwordAuthenticatorBuilder.allowedSaslMechanisms(Collections.singleton(SaslMechanism.PLAIN));
        }

        ClusterOptions clusterOptions =
                clusterOptions(passwordAuthenticatorBuilder.build())
                        .environment(environment);
        Cluster cluster = Cluster.connect(kvNodes.get(0), clusterOptions);
        cluster.waitUntilReady(WAIT_UNTIL_READY_DURATION);
        connectedClusters.put(clusterConnection, cluster);
        return cluster;
    }

    public void disconnect(Component parentComponent, ClusterConnection clusterConnection)
            throws IOException, JSONException {
        Cluster cluster = getCluster(parentComponent, clusterConnection);
        connectedClusters.entrySet().removeIf(entry -> (entry.getValue().equals(cluster)));
        cluster.disconnect();
    }

    public static JSONObject getClusterInfo(ClusterConnection clusterConnection) throws IOException, JSONException {
        // connect to get the cluster map
        Resty restHandler = new Resty();
        // the build in authentication doesn't work as expected, so we add our own header
        //restHandler.authenticate("http://" + cluster, user, password);
        restHandler.withHeader("Authorization", clusterConnection.getEncodedBasicCredentials());
        restHandler.withHeader("Accept", "application/json");

        // TODO: Support other protocols, cleanup
        JSONResource resource =
                restHandler.json("http://" + clusterConnection.getHost() + ":8091" + CLUSTER_MAP_ENDPOINT);

        JSONObject responseContents = resource.object();
        return responseContents;
    }

    public static JSONArray getBucketInfo(ClusterConnection clusterConnection, String bucketEndpoint)
            throws IOException, JSONException {
        // connect to get the cluster map
        Resty restHandler = new Resty();
        // the build in authentication doesn't work as expected, so we add our own header
        //restHandler.authenticate("http://" + cluster, user, password);
        restHandler.withHeader("Authorization", clusterConnection.getEncodedBasicCredentials());
        restHandler.withHeader("Accept", "application/json");

        // TODO: Support other protocols, cleanup
        JSONResource resource =
                restHandler.json("http://" + clusterConnection.getHost() + ":8091" + bucketEndpoint);
        JSONArray responseContents = resource.array();
        return responseContents;
    }

    private List<String> getKVNodes(JSONObject couchbaseConfig) throws JSONException {
        JSONArray nodes = couchbaseConfig.getJSONArray(CLUSTER_MAP_NODES);
        List<String> kvNodes = new ArrayList<>(nodes.length());

        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            JSONArray services = node.getJSONArray(CLUSTER_MAP_SERVICES);
            for (int j = 0; j < services.length(); j++) {
                if (services.get(j).equals(CLUSTER_MAP_KV_SERVICE)) {
                    String nodeHost = node.getString(CLUSTER_MAP_HOSTNAME);
                    kvNodes.add(nodeHost.split(":")[0]);
                    break;
                }
            }
        }
        return kvNodes;
    }
}
