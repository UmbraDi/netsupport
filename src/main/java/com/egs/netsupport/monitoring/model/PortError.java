package com.egs.netsupport.monitoring.model;

public class PortError {
    public final String port;
    public final String reason;

    public PortError(String port, String reason) {
        this.port = port;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return port + " ("  + reason + ")";
    }
}
