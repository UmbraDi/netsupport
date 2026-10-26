package com.egs.netsupport.monitoring;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PingService {
    public boolean ping(String host, int timeoutSeconds) {
        try {
            String timeoutMs = String.valueOf(timeoutSeconds * 1000);
            ProcessBuilder pb = new ProcessBuilder(
                    "ping", "-n", "1", "-w", timeoutMs, host
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds + 2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
