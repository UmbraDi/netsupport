package com.egs.netsupport.monitoring;

public class PortHealthMonitor {
//    private final NodeRepository nodeRepository;
//    private final PortStatusService portStatusService;
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//
//    private static final int CHECK_INTERVAL_MIN = 30;
//    private static final int SWITCH_POOL_SIZE = 5;
//
//    public PortHealthMonitor(NodeRepository nodeRepository, PortStatusService portStatusService) {
//        this.nodeRepository = nodeRepository;
//        this.portStatusService = portStatusService;
//    }
//
//    public void start() {
//        scheduler.scheduleAtFixedRate(
//                this::checkAllSwitches,
//                0,
//                CHECK_INTERVAL_MIN,
//                TimeUnit.MINUTES
//        );
//    }
//
//    public void stop() {
//        scheduler.shutdown();
//    }
//
//    private void checkAllSwitches() {
//        Credentials creds = nodeRepository.getDefaultCredentials();
//        if (creds == null) {
//            System.out.println("[PortHealth] credentials.json not found");
//            return;
//        }
//
//        List<Device> switches = nodeRepository.getMonitoredNodes().stream()
//                .filter(n -> "switch".equals(n.type))
//                .toList();
//
//        System.out.println("[PortHealth] Checking " + switches.size() + " switches...");
//
//        for (Device sw : switches) {
//            List<PortError> errors = portStatusService.getErrDisabledPorts(sw.ip, creds);
//            if (!errors.isEmpty()) {
//                System.out.println("[ALERT] " + sw.name + " (" + sw.ip + ") err-disabled: " + errors);
//            }
//        }
//    }
}
