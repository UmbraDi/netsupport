package com.egs.netsupport.monitoring.model;

public class NodeState {
    public final String ip;
    public int consecutiveFails;
    public boolean up;
    public long lastCheckTime;

    public NodeState(String ip) {
        this.ip = ip;
        this.up = true;
        this.consecutiveFails = 0;
        this.lastCheckTime = 0;
    }
}
