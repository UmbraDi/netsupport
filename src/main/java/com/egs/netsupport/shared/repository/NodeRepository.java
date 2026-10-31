package com.egs.netsupport.shared.repository;

import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.House;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.model.NodesConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NodeRepository {
    private final NodesConfig config;
    private final Credentials defaultCredentials;

    public NodeRepository() {
        this.config = loadConfig();
        this.defaultCredentials = loadCredentials();
    }

    private NodesConfig loadConfig() {
        try (InputStream is = getClass().getClassLoader().
                getResourceAsStream("nodes.json")) {
            if (is == null) {
                throw new IllegalStateException("nodes.json not found in reourses");
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, NodesConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load nodes.json", e);
        }
    }

    private Credentials loadCredentials() {
        Path path = Path.of("credentials.json");
        if (!Files.exists(path)) {
            return null; // работаем без кредов
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(path.toFile(), Credentials.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load credentials.json", e);
        }
    }

    public Credentials getDefaultCredentials() {
        return defaultCredentials;
    }

    // Узлы для пинга
    public List<Node> getMonitoredNodes() {
        if (config.nodes == null) return Collections.emptyList();
        return config.nodes.stream()
                .filter(n -> n.monitor)
                .collect(Collectors.toList());
    }

    // IP в доме
    public List<String> getSwitchIpsForHouse(String houseName) {
        if (config.houses == null) return Collections.emptyList();
        return config.houses.stream()
                .filter(h -> h.name.equalsIgnoreCase(houseName))
                .findFirst()
                .map(h -> h.switchIps != null ? h.switchIps : Collections.<String>emptyList())
                .orElse(Collections.emptyList());
    }

    // Найти узел по IP
    public Optional<Node> getNodeByIp(String ip) {
        if (config.nodes == null) return Optional.empty();
        return config.nodes.stream()
                .filter(n -> n.ip.equals(ip))
                .findFirst();
    }

    // Найти дом по имени
    public Optional<House> getHouseByName(String houseName) {
        if (config.houses == null) return Optional.empty();
        return config.houses.stream()
                .filter(h -> h.name.equalsIgnoreCase(houseName))
                .findFirst();
    }


}
