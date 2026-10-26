package com.egs.netsupport.monitoring;

import com.egs.netsupport.monitoring.model.NodeState;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class NodeMonitor {

    private final NodeRepository nodeRepository;
    private final PingService pingService;
    private final Map<String, NodeState> states = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService pingPool = Executors.newFixedThreadPool(50);

    private static final int FAILS_THRESHOLD = 3;
    private static final int PING_TIMEOUT_SEC = 2;
    private static final int CHECK_INTERVAL_SEC = 30;

    public NodeMonitor(NodeRepository nodeRepository, PingService pingService) {
        this.nodeRepository = nodeRepository;
        this.pingService = pingService;
    }

    public void start() {
        for (Node node: nodeRepository.getMonitoredNodes()) {
            states.put(node.ip, new NodeState(node.ip));
        }
        scheduler.scheduleAtFixedRate(this::checkAllNodes, 0, CHECK_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        pingPool.shutdown();
    }

    private void checkAllNodes() {
        List<Node> nodes = nodeRepository.getMonitoredNodes();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (Node node : nodes) {
            futures.add(pingPool.submit(() -> checkNode(node)));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                // ПОЗЖЕ СДЕЛАТЬ!!!
            }
        }
    }

    private void checkNode(Node node) {
        boolean alive = pingService.ping(node.ip, PING_TIMEOUT_SEC);
        NodeState state = states.get(node.ip);
        if (state == null) return;

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

    private void onNodeDown(Node node) {
        System.out.println("[ALERT] Node DOWN: " + node.name + " (" + node.ip + ")");
    }

    private void onNodeUp(Node node) {
        System.out.println("[RECOVERY] Node UP: " + node.name + " (" + node.ip + ")");
    }

    public Map<String, NodeState> getStates() {
        return states;
    }
}
