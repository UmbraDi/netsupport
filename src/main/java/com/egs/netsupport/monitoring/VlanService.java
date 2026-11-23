package com.egs.netsupport.monitoring;

import com.egs.netsupport.diagnostic.parser.SubscriberVlanParser;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.DeviceRepository;
import com.egs.netsupport.shared.telnet.TelnetService;
import com.egs.netsupport.web.HouseVlanDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VlanService {

    private final DeviceRepository deviceRepository;
    private final SubscriberVlanParser vlanParser = new SubscriberVlanParser();
    private final Credentials credentials;

    public VlanService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
        this.credentials = loadCredentials();
    }

    private Credentials loadCredentials() {
        Path path = Path.of("credentials.json");
        if (!Files.exists(path)) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(path.toFile(), Credentials.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load credentials.json", e);
        }
    }

    /**
     * VLAN по домам: один дом = одна строка, все найденные VLAN собираются в список.
     */
    public List<HouseVlanDto> getHouseVlans() {
        Map<String, HouseVlanDto> byHouse = new LinkedHashMap<>();

        for (Object[] row : deviceRepository.findHouseVlanRows()) {
            String city = (String) row[0];
            String street = (String) row[1];
            String houseNum = (String) row[2];
            Integer vlan = (Integer) row[3];

            String key = city + "|" + street + "|" + houseNum;
            byHouse.computeIfAbsent(key,
                            k -> new HouseVlanDto(city, street, houseNum, new ArrayList<>()))
                    .vlans()
                    .add(vlan);
        }

        return new ArrayList<>(byHouse.values());
    }

    /**
     * Прочитать VLAN с коммутатора и сохранить в БД.
     * Возвращает найденный VLAN или null.
     */
    public Integer refreshVlan(Device device) {
        if (credentials == null) {
            throw new IllegalStateException("credentials.json not found");
        }
        Integer vlan = readVlanFromSwitch(device.ip, credentials);
        if (vlan != null) {
            device.vlan = vlan;
            deviceRepository.save(device);
        }
        return vlan;
    }

    private Integer readVlanFromSwitch(String ip, Credentials creds) {
        TelnetService telnet = new TelnetService();
        try {
            telnet.connect(ip, 23);
            telnet.login(creds.login, creds.password);
            telnet.execute("terminal length 0");

            Integer vlan = vlanParser.findSubscriberVlan(
                    telnet.execute("show mac address-table"));
            if (vlan != null) return vlan;

            return vlanParser.findSubscriberVlan(
                    telnet.execute("show mac-address-table"));
        } catch (IOException e) {
            System.out.println("IOException on " + ip + ": " + e.getMessage());
            return null;
        } finally {
            telnet.disconnect();
        }
    }
}