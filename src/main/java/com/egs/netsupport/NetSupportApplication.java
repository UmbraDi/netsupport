package com.egs.netsupport;

import com.egs.netsupport.shared.repository.NodeRepository;

public class NetSupportApplication {
    public static void main(String[] args) {
        System.out.println("NetSupport started");
        NodeRepository repo = new NodeRepository();
        System.out.println("Monitored nodes: " + repo.getMonitoredNodes().size());
        System.out.println("Switches for 'Финютина 10': " +
                repo.getSwitchIpsForHouse("Финютина 10"));
    }
}