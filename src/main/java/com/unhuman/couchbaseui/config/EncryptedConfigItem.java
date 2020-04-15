package com.unhuman.couchbaseui.config;

public abstract class EncryptedConfigItem {
    private static boolean PermitLossOfEncryptedValues = false;

    static void SetPermitLossOfEncryptedValues(boolean permitLoss) {
        PermitLossOfEncryptedValues = permitLoss;
    }

    static boolean GetPermitLossOfEncryptedValues() {
        return PermitLossOfEncryptedValues;
    }
}
