package com.unhuman.couchbaseui.config;

public enum N1QLQueryRefreshHandling {
    InPlace("In Place"),
    MakeLatest("Make Latest");

    private String displayValue;

    private N1QLQueryRefreshHandling(String displayValue) {
        this.displayValue = displayValue;
    }

    @Override
    public String toString(){
        return displayValue;
    }
}
