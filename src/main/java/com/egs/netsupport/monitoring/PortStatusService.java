package com.egs.netsupport.monitoring;

import com.egs.netsupport.monitoring.model.ErrDisabledDto;
import com.egs.netsupport.monitoring.model.PortError;
import com.egs.netsupport.monitoring.parser.ErrDisabledParser;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.DeviceRepository;
import com.egs.netsupport.shared.telnet.TelnetService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PortStatusService {

    private static final Logger log = LoggerFactory.getLogger(PortStatusService.class);

    private final DeviceRepository deviceRepository;
    private final ErrDisabledParser parser = new ErrDisabledParser();
    private final Credentials credentials;
    private final ExecutorService pool;

    public PortStatusService(DeviceRepository deviceRepository,
                             @Value("${app.monitoring.errdisabled.pool-size:20}") int poolSize) {
        this.deviceRepository = deviceRepository;
        this.credentials = loadCredentials();
        this.pool = Executors.newFixedThreadPool(poolSize, new DaemonThreadFactory());
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdownNow();
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

    /**
     * Опрашивает коммутаторы города (null — все) и возвращает только те,
     * у которых есть err-disabled порты. Опрос идёт параллельно.
     */
    public List<ErrDisabledDto> findErrDisabled(String city) {
        if (credentials == null) {
            log.warn("credentials.json not found, cannot poll switches");
            return List.of();
        }
        return findErrDisabled(deviceRepository.findSwitches(city, null, null));
    }

    /**
     * Опрашивает заданный список коммутаторов параллельно и возвращает только те,
     * у которых есть err-disabled порты. Порядок результата совпадает с порядком входа.
     */
    public List<ErrDisabledDto> findErrDisabled(List<Device> switches) {
        if (credentials == null) {
            log.warn("credentials.json not found, cannot poll switches");
            return List.of();
        }
        if (switches.isEmpty()) {
            return List.of();
        }

        List<Future<List<PortError>>> futures = new ArrayList<>(switches.size());
        for (Device sw : switches) {
            futures.add(pool.submit(() -> getErrDisabledPorts(sw.ip, credentials)));
        }

        List<ErrDisabledDto> result = new ArrayList<>();
        for (int i = 0; i < switches.size(); i++) {
            Device sw = switches.get(i);
            List<PortError> errors;
            try {
                errors = futures.get(i).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                log.warn("Failed to poll {}: {}", sw.ip, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                continue;
            }
            if (!errors.isEmpty()) {
                result.add(new ErrDisabledDto(sw.name, errors));
            }
        }
        return result;
    }

    public List<PortError> getErrDisabledPorts(String switchIp, Credentials creds) {
        TelnetService telnet = new TelnetService();
        try {
            telnet.connect(switchIp, 23);
            telnet.login(creds.login, creds.password);
            telnet.execute("terminal length 0");

            String output = telnet.execute("show interfaces status err-disabled");
            return parser.parse(output);
        } catch (IOException e) {
            log.warn("IOException on {}: {}", switchIp, e.getMessage());
            return Collections.emptyList();
        } finally {
            telnet.disconnect();
        }
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "port-status-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
