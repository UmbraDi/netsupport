package com.egs.netsupport.diagnostic;

import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.diagnostic.model.FdbEntry;
import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.monitoring.PortStatusService;
import com.egs.netsupport.monitoring.model.ErrDisabledDto;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.DeviceRepository;
import com.egs.netsupport.shared.telnet.TelnetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriberDiagnostic {

    private static final Logger log = LoggerFactory.getLogger(SubscriberDiagnostic.class);

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

    public DiagnosticResult diagnose(String city, String street, String houseNum, String mac) {
        String cleanMac = mac == null ? "" : mac.replaceAll("[.:-]", "");
        if (cleanMac.length() < 4) {
            return DiagnosticResult.error("Invalid MAC: " + mac);
        }
        String last4 = cleanMac.substring(cleanMac.length() - 4).toLowerCase();

        List<Device> switches = switchesForHouse(city, street, houseNum);
        if (switches.isEmpty()) {
            return DiagnosticResult.error("House not found: " + street + " " + houseNum);
        }

        if (credentials == null) {
            return DiagnosticResult.error("credentials.json not found");
        }

        // 1. Собираем все записи со всех коммутаторов дома
        List<SwitchEntry> allEntries = new ArrayList<>();

        for (Device sw : switches) {
            if (sw.ip == null || sw.ip.isBlank()) continue;
            TelnetService telnet = new TelnetService();
            try {
                telnet.connect(sw.ip, 23);
                telnet.login(credentials.login, credentials.password);
                telnet.execute("terminal length 0");

                String fdbOutput = telnet.execute("show mac address-table | include " + last4);
                List<FdbEntry> entries = fdbParser.findAll(fdbOutput, last4);

                for (FdbEntry e : entries) {
                    allEntries.add(new SwitchEntry(sw.ip, e));
                }
            } catch (IOException e) {
                log.warn("IOException on {}: {}", sw.ip, e.getMessage());
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
            List<ErrDisabledDto> errors = portStatusService.findErrDisabled(switches);
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
     * Коммутаторы дома по городу/улице/номеру дома.
     */
    private List<Device> switchesForHouse(String city, String street, String houseNum) {
        if (street == null || street.isBlank() || houseNum == null || houseNum.isBlank()) {
            return List.of();
        }
        return deviceRepository.findSwitches(
                (city == null || city.isBlank()) ? null : city.trim(),
                street.trim(),
                houseNum.trim());
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
            log.warn("IOException checking trunk on {} {}: {}", switchIp, port, e.getMessage());
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
