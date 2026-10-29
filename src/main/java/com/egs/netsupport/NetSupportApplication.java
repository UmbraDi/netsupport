package com.egs.netsupport;

import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.util.List;

public class NetSupportApplication {
    public static void main(String[] args) throws InterruptedException {
        TelnetService telnet = new TelnetService();
        System.out.println("TelnetService created");
    }
}