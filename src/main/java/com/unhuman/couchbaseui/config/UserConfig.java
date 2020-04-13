package com.unhuman.couchbaseui.config;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnore;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UserConfig extends ConfigItem {
    private Map<String, List<String>> bucketsCollections;
    private List<String> queries;

    // Do not allow password to serialize, but we retain it during a session
    @JsonIgnore
    private String password;

    UserConfig() {
        bucketsCollections = new HashMap<>();
        queries = new ArrayList<>();

        password = "";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password != null ? password : "";
    }

    /**
     * Upserts a bucket collection
     * @param bucket
     * @param collection
     * @return
     */
    public void upsertBucketCollection(String bucket, String collection) {
        bucket = bucket.trim();
        collection = collection.trim();

        if (!bucketsCollections.containsKey(bucket)) {
            List<String> collections = new ArrayList<>();
            collections.add(collection);
            bucketsCollections.put(bucket, collections);
            setDirty();
        } else {
            List<String> collections = bucketsCollections.get(bucket);
            if (!collections.contains(collection)) {
                collections.add(collection);
                setDirty();
            }
        }
    }

    /**
     * Upserts a query
     * @param query
     * @return
     */
    public void upsertQuery(String query) {
        query = query.trim();

        if (!queries.contains(query)) {
            queries.add(query);
            setDirty();
        }
    }

    @JsonIgnore
    public List<String> getBuckets() {
        List<String> buckets = new ArrayList<>(bucketsCollections.keySet());
        buckets.sort(String::compareTo);
        return buckets;
    }

    public List<String> getQueries() {
        return queries;
    }

    public List<String> getBucketCollections(String bucket) {
        List<String> collections = new ArrayList<>();

        bucket = bucket.trim();
        if (bucketsCollections.get(bucket) != null) {
            collections.addAll(bucketsCollections.get(bucket));
        }

        collections.sort(String::compareTo);
        return collections;
    }
}
