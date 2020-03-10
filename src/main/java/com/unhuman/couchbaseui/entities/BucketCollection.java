package com.unhuman.couchbaseui.entities;

import static com.unhuman.couchbaseui.utils.Utilities.stringIsNullOrEmpty;
import static com.unhuman.couchbaseui.utils.Utilities.trimString;

public class BucketCollection {
    private String bucketName;
    private String collectionName;

    public BucketCollection(String bucketName, String collectionName) {
        this.bucketName = trimString(bucketName);
        this.collectionName = trimString(collectionName);
    }

    public String getBucketName() {
        return bucketName;
    }

    public boolean hasCollectionName() {
        return !stringIsNullOrEmpty(collectionName);
    }

    public String getCollectionName() {
        return collectionName;
    }
}
