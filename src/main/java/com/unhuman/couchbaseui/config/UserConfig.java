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

    // users that haven't been validated should not be serialized when saving.
    @JsonIgnore
    private boolean validated;

    @Encrypt
    private String password;

    private UserConfig() {
        // When deserializing, this is set to true
        this.validated = true;
        bucketsCollections = new HashMap<>();
        queries = new ArrayList<>();

        password = "";
    }

    public static UserConfig createNewUserConfig() {
        UserConfig userConfig = new UserConfig();
        // override that this is not validated yet
        userConfig.setValidated(false);
        return userConfig;
    }

    @JsonIgnore
    public boolean isValidated() {
        return validated;
    }

    @JsonIgnore
    public void setValidated(boolean validated) {
        this.validated = validated;
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
    public List<String> getBucketNames() {
        List<String> bucketNames = new ArrayList<>(bucketsCollections.keySet());
        Collections.sort(bucketNames);
        return Collections.unmodifiableList(bucketNames);
    }

    public void removeBucket(String bucketName) {
        bucketsCollections.remove(bucketName);
    }

    public List<String> getQueries() {
        return queries;
    }

    /**
     * gets a copy of the bucket collections
     * @param bucketName
     * @return
     */
    public List<String> getBucketCollections(String bucketName) {
        List<String> collections = new ArrayList<>();

        bucketName = bucketName.trim();
        if (bucketsCollections.get(bucketName) != null) {
            collections.addAll(bucketsCollections.get(bucketName));
        }

        Collections.sort(collections);
        return Collections.unmodifiableList(collections);
    }

    public void removeBucketCollection(String bucketName, String collection) {
        bucketName = bucketName.trim();
        collection = collection.trim();

        if (bucketsCollections.get(bucketName) != null) {
            bucketsCollections.get(bucketName).remove(collection);
        }

    }
}
