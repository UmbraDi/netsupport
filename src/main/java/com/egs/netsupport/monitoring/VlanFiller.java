package com.egs.netsupport.monitoring;

import com.egs.netsupport.diagnostic.parser.SubscriberVlanParser;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.io.IOException;
import java.util.List;

/**
 * Заполняет VLAN коммутаторов в net.devices.
 *
 * Для каждого коммутатора:
 *   1. show mac address-table (для старых IOS — fallback show mac-address-table)
 *   2. берём любую DYNAMIC-запись с трёхзначным VLAN на абонентском порту
 *      (не trunk/uplink/port-channel)
 *   3. записываем VLAN в net.devices.vlan по IP коммутатора
 *
 * Запуск: java com.egs.netsupport.monitoring.VlanFiller
 */
public class VlanFiller {

    private final NodeRepository repository;
    private final TelnetService telnet;
    private final SubscriberVlanParser vlanParser = new SubscriberVlanParser();

    public VlanFiller(NodeRepository repository, TelnetService telnet) {
        this.repository = repository;
        this.telnet = telnet;
    }

    public static void main(String[] args) {
        NodeRepository repository = new NodeRepository();
        VlanFiller filler = new VlanFiller(repository, new TelnetService());
        try {
            filler.fillAllSwitches();
        } finally {
            NodeRepository.shutdown();
        }
    }

    public void fillAllSwitches() {
        Credentials creds = repository.getDefaultCredentials();
        if (creds == null) {
            System.out.println("credentials.json not found");
            return;
        }

        List<Device> switches = repository.getMonitoredNodes().stream()
                .filter(d -> "switch".equals(d.type))
                .filter(d -> d.ip != null && !d.ip.isBlank())
                .toList();

        System.out.println("Filling VLAN for " + switches.size() + " switches...\n");

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (Device sw : switches) {
            Integer vlan = readSubscriberVlan(sw.ip, creds);

            if (vlan == null) {
                System.out.printf("%-15s VLAN not found (skip)%n", sw.ip);
                skipped++;
                continue;
            }

            try {
                int rows = repository.updateVlan(sw.ip, vlan);
                if (rows > 0) {
                    System.out.printf("%-15s vlan=%d -> saved%n", sw.ip, vlan);
                    updated++;
                } else {
                    System.out.printf("%-15s vlan=%d -> device not found in DB%n", sw.ip, vlan);
                    skipped++;
                }
            } catch (RuntimeException e) {
                System.out.printf("%-15s vlan=%d -> DB error: %s%n", sw.ip, vlan, e.getMessage());
                failed++;
            }
        }

        System.out.printf("%nDone: updated=%d, skipped=%d, failed=%d%n", updated, skipped, failed);
    }

    /**
     * Читает VLAN абонентского порта с коммутатора.
     * Сначала пробуем современную команду, для старых IOS — вариант с дефисом.
     */
    private Integer readSubscriberVlan(String switchIp, Credentials creds) {
        try {
            telnet.connect(switchIp, 23);
            telnet.login(creds.login, creds.password);
            telnet.execute("terminal length 0");


            Integer vlan = vlanParser.findSubscriberVlan(
                    telnet.execute("show mac address-table"));
            if (vlan != null) return vlan;

            // старые коммутаторы: show mac-address-table
            return vlanParser.findSubscriberVlan(
                    telnet.execute("show mac-address-table"));
        } catch (IOException e) {
            System.out.println("IOException on " + switchIp + ": " + e.getMessage());
            return null;
        } finally {
            telnet.disconnect();
        }
    }
}
