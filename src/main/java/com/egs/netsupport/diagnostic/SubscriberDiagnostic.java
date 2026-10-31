package com.egs.netsupport.diagnostic;

import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.diagnostic.model.FdbEntry;
import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubscriberDiagnostic {

    private final NodeRepository nodeRepository;
    private final TelnetService telnetService;
    private final FdbParser fdbParser = new FdbParser();
    private final InterfaceParser interfaceParser = new InterfaceParser();

    public SubscriberDiagnostic(NodeRepository nodeRepository, TelnetService telnetService) {
        this.nodeRepository = nodeRepository;
        this.telnetService = telnetService;
    }

    public DiagnosticResult diagnose(String houseName, String mac) {
        String cleanMac = mac.replaceAll("[.:-]", "");
        if (cleanMac.length() < 4) {
            return DiagnosticResult.error("Invalid MAC: " + mac);
        }
        String last4 = cleanMac.substring(cleanMac.length() - 4).toLowerCase();

        List<String> switchIps = nodeRepository.getSwitchIpsForHouse(houseName);
        if (switchIps.isEmpty()) {
            return DiagnosticResult.error("House not found: " + houseName);
        }

        Credentials creds = nodeRepository.getDefaultCredentials();
        if (creds == null) {
            return DiagnosticResult.error("credentials.json not found");
        }

        // 1. Собираем все записи со всех коммутаторов
        List<SwitchEntry> allEntries = new ArrayList<>();

        for (String switchIp : switchIps) {
            if (switchIp == null || switchIp.isBlank()) continue;
            try {
                telnetService.connect(switchIp, 23);
                telnetService.login(creds.login, creds.password);
                telnetService.execute("terminal length 0");

                String fdbOutput = telnetService.execute("show mac address-table | include " + last4);
                List<FdbEntry> entries = fdbParser.findAll(fdbOutput, last4);

                // запоминаем, с какого коммутатора пришла каждая запись
                for (FdbEntry e : entries) {
                    allEntries.add(new SwitchEntry(switchIp, e));
                }
            } catch (IOException e) {
                System.out.println("IOException on " + switchIp + ": " + e.getMessage());
            } finally {
                telnetService.disconnect();
            }
        }

        // 2. Фильтруем: трёхзначный VLAN и порт не транковый
        List<SwitchEntry> candidates = new ArrayList<>();
        for (SwitchEntry se : allEntries) {
            if (!se.entry.isSubscriberVlan()) continue;
            if (isTrunkPort(se.switchIp, se.entry.port, creds)) continue;
            candidates.add(se);
        }

        if (candidates.isEmpty()) {
            return DiagnosticResult.notFound();
        }

        // 3. Берём первую подходящую запись и парсим show interface
        SwitchEntry chosen = candidates.get(0);
        try {
            telnetService.connect(chosen.switchIp, 23);
            telnetService.login(creds.login, creds.password);
            telnetService.execute("terminal length 0");

            String ifOutput = telnetService.execute("show interface " + chosen.entry.port);
            PortDetails details = interfaceParser.parse(ifOutput);

            DiagnosticResult result = new DiagnosticResult();
            result.found = true;
            result.switchIp = chosen.switchIp;
            result.port = chosen.entry.port;
            result.portDetails = details;
            return result;
        } catch (IOException e) {
            return DiagnosticResult.error("Failed to get interface details: " + e.getMessage());
        } finally {
            telnetService.disconnect();
        }
    }

    /**
     * Проверяет, является ли порт транковым.
     * Подключается к коммутатору (если ещё не подключён), выполняет show interfaces <port> status
     * и смотрит, есть ли в выводе "trunk".
     */
    private boolean isTrunkPort(String switchIp, String port, Credentials creds) {
        try {
            telnetService.connect(switchIp, 23);
            telnetService.login(creds.login, creds.password);
            telnetService.execute("terminal length 0");

            String statusOutput = telnetService.execute("show interfaces " + port + " status");
            return statusOutput.toLowerCase().contains("trunk");
        } catch (IOException e) {
            System.out.println("IOException checking trunk on " + switchIp + " " + port + ": " + e.getMessage());
            return false;
        } finally {
            telnetService.disconnect();
        }
    }

    /**
     * Пара «коммутатор + запись FDB».
     */
    private static class SwitchEntry {
        final String switchIp;
        final FdbEntry entry;

        SwitchEntry(String switchIp, FdbEntry entry) {
            this.switchIp = switchIp;
            this.entry = entry;
        }
    }
}
