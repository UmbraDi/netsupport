package com.egs.netsupport;

import com.egs.netsupport.diagnostic.SubscriberDiagnostic;
import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.monitoring.PortHealthMonitor;
import com.egs.netsupport.monitoring.PortStatusService;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.util.List;

public class NetSupportApplication {
    public static void main(String[] args) throws InterruptedException {
        NodeRepository repo = new NodeRepository();
        TelnetService telnet = new TelnetService();
        PortStatusService portStatus = new PortStatusService(telnet);
        PortHealthMonitor healthMonitor = new PortHealthMonitor(repo, portStatus);

        healthMonitor.start();

        Thread.sleep(10_000);
        healthMonitor.stop();
    }
}