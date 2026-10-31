package com.egs.netsupport;

import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.util.List;

public class NetSupportApplication {
    public static void main(String[] args) throws InterruptedException {
        FdbParser parser = new FdbParser();

        String output = "320    50ff.2096.d801    DYNAMIC     Gi1/0/25";
        String port = parser.findPort(output, "d801");
        System.out.println("Port: " + port); // ожидаем Gi1/0/25

        String notFound = parser.findPort(output, "ffff");
        System.out.println("Not found: " + notFound);
    }
}