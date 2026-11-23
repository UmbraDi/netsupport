package com.egs.netsupport.diagnostic;

import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.diagnostic.model.FdbEntry;
import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.monitoring.PortStatusService;
import com.egs.netsupport.monitoring.model.PortError;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.DeviceRepository;
import com.egs.netsupport.shared.telnet.TelnetService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriberDiagnostic {

    private final DeviceRepository deviceRepository;
    private final PortStatusService portStatusService;
    private final FdbParser fdbParser = new FdbParser();
    private final InterfaceParser interfaceParser = new InterfaceParser();
    private final Credentials credentials;

    public SubscriberDiagnostic(DeviceRepository deviceRepository,
                                PortStatusService portStatusService) {
        this.deviceRepository = deviceRepository;
        this.portStatusService = portStatusService;
        this.credentials = loadCredentials();
    }

    private Credentials loadCredentials() {
        Path path = Path.of("credentials.json");
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(path.toFile(), Credentials.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load credentials.json", e);
        }
    }

    public DiagnosticResult diagnose(String houseName, String mac) {
        String cleanMac = mac.replaceAll("[.:-]", "");
        if (cleanMac.length() < 4) {
            return DiagnosticResult.error("Invalid MAC: " + mac);
        }
        String last4 = cleanMac.substring(cleanMac.length() - 4).toLowerCase();

        List<String> switchIps = switchIpsForHouse(houseName);
        if (switchIps.isEmpty()) {
            return DiagnosticResult.error("House not found: " + houseName);
        }

        if (credentials == null) {
            return DiagnosticResult.error("credentials.json not found");
        }

        // 1. Собираем все записи со всех коммутаторов дома
        List<SwitchEntry> allEntries = new ArrayList<>();

        for (String switchIp : switchIps) {
            if (switchIp == null || switchIp.isBlank()) continue;
            TelnetService telnet = new TelnetService();
            try {
                telnet.connect(switchIp, 23);
                telnet.login(credentials.login, credentials.password);
                telnet.execute("terminal length 0");

                String fdbOutput = telnet.execute("show mac address-table | include " + last4);
                List<FdbEntry> entries = fdbParser.findAll(fdbOutput, last4);

                for (FdbEntry e : entries) {
                    allEntries.add(new SwitchEntry(switchIp, e));
                }
            } catch (IOException e) {
                System.out.println("IOException on " + switchIp + ": " + e.getMessage());
            } finally {
                telnet.disconnect();
            }
        }

        // 2. Фильтруем: трёхзначный VLAN и порт не транковый
        List<SwitchEntry> candidates = new ArrayList<>();
        for (SwitchEntry se : allEntries) {
            if (!se.entry.isSubscriberVlan()) continue;
            if (isTrunkPort(se.switchIp, se.entry.port)) continue;
            candidates.add(se);
        }

        if (candidates.isEmpty()) {
            // MAC не найден — проверяем err-disabled на коммутаторах дома
            List<PortError> errors = new ArrayList<>();
            for (String switchIp : switchIps) {
                if (switchIp == null || switchIp.isBlank()) continue;
                errors.addAll(portStatusService.getErrDisabledPorts(switchIp, credentials));
            }

            if (!errors.isEmpty()) {
                return DiagnosticResult.error("MAC not found. Err-disabled ports: " + errors);
            }
            return DiagnosticResult.notFound();
        }

        // 3. Берём первую подходящую запись и парсим show interface
        SwitchEntry chosen = candidates.get(0);
        TelnetService telnet = new TelnetService();
        try {
            telnet.connect(chosen.switchIp, 23);
            telnet.login(credentials.login, credentials.password);
            telnet.execute("terminal length 0");

            String ifOutput = telnet.execute("show interface " + chosen.entry.port);
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
            telnet.disconnect();
        }
    }

    /**
     * IP коммутаторов дома. "Финютина 10" -> street='Финютина', houseNum='10'.
     */
    private List<String> switchIpsForHouse(String houseName) {
        if (houseName == null || houseName.isBlank()) {
            return List.of();
        }
        String trimmed = houseName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');

        List<Device> switches = deviceRepository.findByTypeAndIpIsNotNullOrderByIp("switch");

        if (lastSpace <= 0) {
            // не смогли разобрать — ищем только по houseNum
            return switches.stream()
                    .filter(d -> trimmed.equalsIgnoreCase(d.houseNum))
                    .map(d -> d.ip)
                    .toList();
        }

        String street = trimmed.substring(0, lastSpace).trim();
        String houseNum = trimmed.substring(lastSpace + 1).trim();

        return switches.stream()
                .filter(d -> d.street != null
                        && d.street.equalsIgnoreCase(street)
                        && houseNum.equalsIgnoreCase(d.houseNum))
                .map(d -> d.ip)
                .toList();
    }

    /**
     * Проверяет, является ли порт транковым.
     */
    private boolean isTrunkPort(String switchIp, String port) {
        TelnetService telnet = new TelnetService();
        try {
            telnet.connect(switchIp, 23);
            telnet.login(credentials.login, credentials.password);
            telnet.execute("terminal length 0");

            String statusOutput = telnet.execute("show interfaces " + port + " status");
            return statusOutput.toLowerCase().contains("trunk");
        } catch (IOException e) {
            System.out.println("IOException checking trunk on " + switchIp + " " + port + ": " + e.getMessage());
            return false;
        } finally {
            telnet.disconnect();
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