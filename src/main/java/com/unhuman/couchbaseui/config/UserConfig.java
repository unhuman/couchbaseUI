package com.unhuman.couchbaseui.config;

import com.codingrodent.jackson.crypto.Encrypt;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.*;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UserConfig extends EncryptedConfigItem {
    private Map<String, List<String>> bucketsCollections;
    private List<String> queries;

    @Encrypt
    private String password;

    UserConfig() {
        bucketsCollections = new HashMap<>();
        queries = new ArrayList<>();

        password = "";
    }

    public void setPassword(Object password) {
        // ignore non-string values (failed decryption)
        if (password instanceof String) {
            this.password = (String) password;
        } else if (!GetPermitLossOfEncryptedValues()) {
            throw new PasswordDecryptionException("Can't deserialize encrypted value");
        }
    }

    @Encrypt
    public String getPassword() {
        return (GetPermitLossOfEncryptedValues() || this.password == null) ? "" : password;
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
        } else {
            List<String> collections = bucketsCollections.get(bucket);
            if (!collections.contains(collection)) {
                collections.add(collection);
            }
        }
    }

    /**
     * Upserts a query
     * @param query
     * @return
     */
    public void upsertQuery(N1QLQueryRefreshHandling n1QLQueryRefreshHandling, String query) {
        query = query.trim();

        switch (n1QLQueryRefreshHandling) {
            case InPlace:
                if (!queries.contains(query)) {
                    queries.add(query);
                }
                break;
            case MakeLatest:
                queries.remove(query);
                queries.add(query);
                break;
            default:
                throw new RuntimeException("Unknown query handling: " + n1QLQueryRefreshHandling);
        }
    }

    @JsonIgnore
    public List<String> getBuckets() {
        List<String> buckets = new ArrayList<>(bucketsCollections.keySet());
        Collections.sort(buckets);
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

        Collections.sort(collections);
        return collections;
    }
}
