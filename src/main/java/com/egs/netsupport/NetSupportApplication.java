package com.egs.netsupport;

import com.egs.netsupport.diagnostic.SubscriberDiagnostic;
import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.monitoring.PortHealthMonitor;
import com.egs.netsupport.monitoring.PortStatusService;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class NetSupportApplication {
    public static void main(String[] args) {
        NodeRepository repo = new NodeRepository();
        TelnetService telnet = new TelnetService();

        List<String> switchIps = repo.getMonitoredNodes().stream()
                .filter(n -> "switch".equals(n.type))
                .map(n -> n.ip)
                .toList();

        Credentials creds = repo.getDefaultCredentials();
        if (creds == null) {
            System.out.println("credentials.json not found");
            return;
        }

        System.out.println("Measuring on " + switchIps.size() + " switches...\n");

        long totalStart = System.currentTimeMillis();

        for (String ip : switchIps) {
            long start = System.currentTimeMillis();

            try {
                telnet.connect(ip, 23);
                long connectTime = System.currentTimeMillis() - start;

                long loginStart = System.currentTimeMillis();
                telnet.login(creds.login, creds.password);
                long loginTime = System.currentTimeMillis() - loginStart;

                long cmdStart = System.currentTimeMillis();
                telnet.execute("terminal length 0");
                String output = telnet.execute("show interfaces status err-disabled");
                long cmdTime = System.currentTimeMillis() - cmdStart;

                long total = System.currentTimeMillis() - start;

                System.out.printf("%-15s connect=%4dms  login=%4dms  cmd=%4dms  total=%4dms  output_len=%d%n",
                        ip, connectTime, loginTime, cmdTime, total, output.length());

            } catch (IOException e) {
                long total = System.currentTimeMillis() - start;
                System.out.printf("%-15s FAILED after %dms: %s%n", ip, total, e.getMessage());
            } finally {
                telnet.disconnect();
            }
        }

        long totalTime = System.currentTimeMillis() - totalStart;
        System.out.printf("%nTotal: %d ms for %d switches, avg = %d ms/switch%n",
                totalTime, switchIps.size(), totalTime / switchIps.size());
    }
}