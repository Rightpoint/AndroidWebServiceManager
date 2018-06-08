package com.raizlabs.webservicemanager.ssl;

public enum TLS {

    VERSION_1("TLS"),VERSION_1_1("TLSv1.1"),VERSION_1_2("TLSv1.2");

    TLS(String version) {
        this.version = version;
    }

    private String version;

    public String getVersion() {
        return version;
    }
}
