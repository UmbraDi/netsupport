package com.egs.netsupport;

import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;

import java.util.List;

public class NetSupportApplication {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("NetSupport started");
        NodeRepository repo = new NodeRepository();
        PingService pingService = new PingService();
        List<Node> nodes = repo.getMonitoredNodes();
        System.out.println("Checking " + nodes.size() + " nodes...\n");

        for (Node node : nodes) {
            boolean alive = pingService.ping(node.ip, 2);
            System.out.printf("%-20s %-15s %s%n",
                    node.name, node.ip, alive ? "UP" : "DOWN");
        }
    }
}