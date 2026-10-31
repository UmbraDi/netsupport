package com.egs.netsupport.diagnostic.model;

public class FdbEntry {
    public String vlan;
    public String mac;
    public String type;
    public String port;

    public FdbEntry(String vlan, String mac, String type, String port) {
        this.vlan = vlan;
        this.mac = mac;
        this.type = type;
        this.port = port;
    }

    public boolean isSubscriberVlan() {
        if (vlan == null || vlan.length() != 3) return false;
        for (char c : vlan.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
