package com.egs.netsupport.monitoring;

import com.egs.netsupport.monitoring.model.NodeState;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.ArrayList;

/**
 * Мониторинг доступности коммутаторов (ping) с антифлаппингом.
 * Включается свойством app.monitoring.ping.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.monitoring.ping.enabled", havingValue = "true")
public class NodeMonitor {

    private static final Logger log = LoggerFactory.getLogger(NodeMonitor.class);

    private static final int FAILS_THRESHOLD = 3;
    private static final int PING_TIMEOUT_SEC = 2;

    private final DeviceRepository deviceRepository;
    private final PingService pingService = new PingService();

    private final Map<String, NodeState> states = new ConcurrentHashMap<>();
    private final ExecutorService pingPool = Executors.newFixedThreadPool(50);

    public NodeMonitor(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Scheduled(fixedDelayString = "${app.monitoring.ping.interval-ms:30000}")
    public void checkAllNodes() {
        List<Device> nodes = deviceRepository.findByTypeAndIpIsNotNullOrderByIp("switch");

        List<Future<?>> futures = new ArrayList<>();
        for (Device node : nodes) {
            futures.add(pingPool.submit(() -> checkNode(node)));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Ping check failed: {}", e.getMessage());
            }
        }
    }

    private void checkNode(Device node) {
        boolean alive = pingService.ping(node.ip, PING_TIMEOUT_SEC);
        NodeState state = states.computeIfAbsent(node.ip, NodeState::new);

        state.lastCheckTime = System.currentTimeMillis();

        if (alive) {
            state.consecutiveFails = 0;
            if (!state.up) {
                state.up = true;
                onNodeUp(node);
            }
        } else {
            state.consecutiveFails++;
            if (state.up && state.consecutiveFails >= FAILS_THRESHOLD) {
                state.up = false;
                onNodeDown(node);
            }
        }
    }

    private void onNodeDown(Device node) {
        log.warn("[ALERT] Node DOWN: {} ({})", node.name, node.ip);
    }

    private void onNodeUp(Device node) {
        log.info("[RECOVERY] Node UP: {} ({})", node.name, node.ip);
    }

    public Map<String, NodeState> getStates() {
        return states;
    }
}
