package com.unhuman.couchbaseui.couchbase;

import com.couchbase.client.core.env.PasswordAuthenticator;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.unhuman.couchbaseui.entities.BucketCollection;
import com.unhuman.couchbaseui.entities.ClusterConfig;
import com.unhuman.couchbaseui.utils.Utilities;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.ClusterOptions.clusterOptions;

public class CouchbaseClientManager {
    private static final String CLUSTER_MAP_ENDPOINT = "/pools/nodes";
    private static final String CLUSTER_MAP_KV_SERVICE = "kv";
    private static final String CLUSTER_MAP_HOSTNAME = "hostname";
    private static final String CLUSTER_MAP_NODES = "nodes";
    private static final String CLUSTER_MAP_SERVICES = "services";

    Map<ClusterConfig, Cluster> connectedClusters;

    public CouchbaseClientManager() {
        this.connectedClusters = new HashMap<>();
    }

    public Collection getBucketCollection(ClusterConfig clusterConfig, BucketCollection bucketCollection)
            throws IOException, JSONException {
        if (Utilities.stringIsNullOrEmpty(bucketCollection.getBucketName())) {
            throw new RuntimeException("Bucket Name must be provided");
        }

        Bucket bucket = getCluster(clusterConfig).bucket(bucketCollection.getBucketName());
        return (bucketCollection.hasCollectionName())
                ? bucket.collection(bucketCollection.getCollectionName())
                : bucket.defaultCollection();
    }

    public Cluster getCluster(ClusterConfig clusterConfig) throws IOException, JSONException {
        // Check the local cache
        if (connectedClusters.containsKey(clusterConfig)) {
            return connectedClusters.get(clusterConfig);
        }

        // connect to get the cluster map
        Resty restHandler = new Resty();
        // the build in authentication doesn't work as expected, so we add our own header
        //restHandler.authenticate("http://" + cluster, user, password);
        restHandler.withHeader("Authorization", clusterConfig.getEncodedBasicCredentials());
        restHandler.withHeader("Accept", "application/json");

        // TODO: Support other protocols, cleanup
        JSONResource resource = restHandler.json("http://" + clusterConfig.getHost() + ":8091" + CLUSTER_MAP_ENDPOINT);
        JSONObject responseContents = resource.object();
        List<String> kvNodes = getKVNodes(responseContents);

        // connect to the cluster (right now, it just picks first item out of cluster map)
        ClusterOptions clusterOptions =
                clusterOptions(PasswordAuthenticator.create(clusterConfig.getUser(), clusterConfig.getPassword()));
        Cluster cluster = Cluster.connect(kvNodes.get(0), clusterOptions);
        connectedClusters.put(clusterConfig, cluster);
        return cluster;
    }

    private List<String> getKVNodes(JSONObject couchbaseConfig) throws JSONException {
        JSONArray nodes = couchbaseConfig.getJSONArray(CLUSTER_MAP_NODES);

        List<String> kvNodes = new ArrayList<>(nodes.length());

        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = (JSONObject)nodes.get(i);
            JSONArray services = node.getJSONArray(CLUSTER_MAP_SERVICES);
            for (int j = 0; j < services.length(); j++) {
                if (services.get(j).equals(CLUSTER_MAP_KV_SERVICE)) {
                    String nodeHost = (String) node.get(CLUSTER_MAP_HOSTNAME);
                    kvNodes.add(nodeHost.split(":")[0]);
                    break;
                }
            }
        }
        return kvNodes;
    }
}
