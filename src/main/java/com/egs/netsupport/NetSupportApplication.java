package com.egs.netsupport;

import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.shared.repository.NodeRepository;

public class NetSupportApplication {
    public static void main(String[] args) {
        System.out.println("NetSupport started");
        NodeRepository repo = new NodeRepository();
        System.out.println("Monitored nodes: " + repo.getMonitoredNodes().size());
        System.out.println("Switches for 'Финютина 10': " +
                repo.getSwitchIpsForHouse("Финютина 10"));
        PingService pingService = new PingService();
        System.out.println("8.8.8.8: " + pingService.ping("8.8.8.8", 2));
        System.out.println("172.17.10.21: " + pingService.ping("172.17.10.21", 2));
        System.out.println("172.17.12.21: " + pingService.ping("172.17.12.21", 2));
    }
}