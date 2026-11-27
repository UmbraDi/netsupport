package com.egs.netsupport.monitoring;

import com.egs.netsupport.monitoring.model.ErrDisabledDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Периодический поиск err-disabled портов на всех коммутаторах.
 * Включается свойством app.monitoring.errdisabled.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.monitoring.errdisabled.enabled", havingValue = "true")
public class PortHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(PortHealthMonitor.class);

    private final PortStatusService portStatusService;

    public PortHealthMonitor(PortStatusService portStatusService) {
        this.portStatusService = portStatusService;
    }

    @Scheduled(
            fixedDelayString = "${app.monitoring.errdisabled.interval-ms:1800000}",
            initialDelayString = "${app.monitoring.errdisabled.initial-delay-ms:60000}"
    )
    public void checkAllSwitches() {
        log.info("[PortHealth] Checking switches for err-disabled ports...");
        List<ErrDisabledDto> result = portStatusService.findErrDisabled((String) null);
        for (ErrDisabledDto dto : result) {
            log.warn("[ALERT] {} err-disabled: {}", dto.name, dto.errors);
        }
    }
}
