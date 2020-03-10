package com.unhuman.couchbaseui.entities;

import java.util.Base64;
import java.util.Objects;

import static com.unhuman.couchbaseui.utils.Utilities.stringIsNullOrEmpty;
import static com.unhuman.couchbaseui.utils.Utilities.trimString;

public class ClusterConfig {
    private final String host;
    private final String user;
    private final String password;

    public ClusterConfig(String host, String user, String password) {
        this.host = trimString(host);
        this.user = trimString(user);
        this.password = trimString(password);

        if (stringIsNullOrEmpty(host)) {
            throw new RuntimeException("Couchbase Cluster must be provided");
        }

        if (stringIsNullOrEmpty(user)) {
            throw new RuntimeException("User must be provided");
        }

        if (stringIsNullOrEmpty(password)) {
            throw new RuntimeException("Password must be provided");
        }
    }

    public String getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getEncodedBasicCredentials() {
        byte[] encoded = Base64.getEncoder().encode((user + ":" + password).getBytes());
        return "Basic " + new String(encoded);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterConfig that = (ClusterConfig) o;
        return host.equals(that.host) &&
                user.equals(that.user) &&
                password.equals(that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, user, password);
    }
}
