package com.egs.netsupport;

import com.egs.netsupport.diagnostic.SubscriberDiagnostic;
import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.util.List;

public class NetSupportApplication {
    public static void main(String[] args) throws InterruptedException {
        NodeRepository repo = new NodeRepository();
        TelnetService telnet = new TelnetService();
        SubscriberDiagnostic diag = new SubscriberDiagnostic(repo, telnet);


        // замени на реальный дом и MAC
        DiagnosticResult result = diag.diagnose("Финютина 10", "99a8");
        System.out.println(result);
    }
}