package com.egs.netsupport.diagnostic.model;

public class DiagnosticResult {
    public boolean found;
    public String switchIp;
    public String port;
    public PortDetails portDetails;
    public String error;

    public static DiagnosticResult notFound() {
        DiagnosticResult r = new DiagnosticResult();
        r.found = false;
        return r;
    }

    public static DiagnosticResult error(String message) {
        DiagnosticResult r = new DiagnosticResult();
        r.found = false;
        r.error = message;
        return r;
    }

    @Override
    public String toString() {
        if (!found) {
            return error != null ? "ERROR: " + error : "NOT FOUND";
        }
        return "Switch: " + switchIp + ", Port: " + port + ", " + portDetails;
    }
}
