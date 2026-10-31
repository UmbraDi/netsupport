package com.egs.netsupport.diagnostic.parser;

import com.egs.netsupport.diagnostic.model.PortDetails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterfaceParser {

    // Gi1/0/25 is up, line protocol is up (connected)
    private static final Pattern STATUS_PATTERN =
            Pattern.compile("line protocol is (\\w+)");

    // BW 100000 Kbit
    private static final Pattern BW_PATTERN =
            Pattern.compile("BW (\\d+) Kbit");

    // 0 input errors, 0 CRC, 0 frame, 0 overrun, 0 ignored
    private static final Pattern INPUT_ERRORS_PATTERN =
            Pattern.compile("(\\d+) input errors, (\\d+) CRC");

    // 0 output errors, 0 collisions, 0 interface resets
    private static final Pattern OUTPUT_ERRORS_PATTERN =
            Pattern.compile("(\\d+) output errors, (\\d+) collisions");

    // Total output drops: 125313818
    private static final Pattern OUTPUT_DROPS_PATTERN =
            Pattern.compile("Total output drops: (\\d+)");

    public PortDetails parse(String output) {
        if (output == null || output.isBlank()) {
            return new PortDetails();
        }

        PortDetails details = new PortDetails();

        details.linkUp = parseLinkUp(output);
        details.speedMbps = parseSpeedMbps(output);
        details.inputErrors = parseInputErrors(output);
        details.crcErrors = parseCrcErrors(output);
        details.outputErrors = parseOutputErrors(output);
        details.collisions = parseCollisions(output);
        details.outputDrops = parseOutputDrops(output);

        return details;
    }

    private boolean parseLinkUp(String output) {
        Matcher m = STATUS_PATTERN.matcher(output);
        if (m.find()) {
            String status = m.group(1).toLowerCase();
            return "up".equals(status);
        }
        return false;
    }

    private int parseSpeedMbps(String output) {
        Matcher m = BW_PATTERN.matcher(output);
        if (m.find()) {
            int kbit = Integer.parseInt(m.group(1));
            return kbit / 1000; // Kbit → Mbps
        }
        return 0;
    }

    private long parseInputErrors(String output) {
        Matcher m = INPUT_ERRORS_PATTERN.matcher(output);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return 0;
    }

    private long parseCrcErrors(String output) {
        Matcher m = INPUT_ERRORS_PATTERN.matcher(output);
        if (m.find()) {
            return Long.parseLong(m.group(2));
        }
        return 0;
    }

    private long parseOutputErrors(String output) {
        Matcher m = OUTPUT_ERRORS_PATTERN.matcher(output);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return 0;
    }

    private long parseCollisions(String output) {
        Matcher m = OUTPUT_ERRORS_PATTERN.matcher(output);
        if (m.find()) {
            return Long.parseLong(m.group(2));
        }
        return 0;
    }

    private long parseOutputDrops(String output) {
        Matcher m = OUTPUT_DROPS_PATTERN.matcher(output);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return 0;
    }
}
