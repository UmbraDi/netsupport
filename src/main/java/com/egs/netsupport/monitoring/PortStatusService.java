package com.egs.netsupport.monitoring;

import com.egs.netsupport.monitoring.model.PortError;
import com.egs.netsupport.monitoring.parser.ErrDisabledParser;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PortStatusService {

    private final TelnetService telnetService;
    private final ErrDisabledParser parser = new ErrDisabledParser();

    public PortStatusService(TelnetService telnetService) {
        this.telnetService = telnetService;
    }

    public List<PortError> getErrDisabledPorts(String switchIp, Credentials creds) {
        try {
            telnetService.connect(switchIp, 23);
            telnetService.login(creds.login, creds.password);
            telnetService.execute("terminal length 0");

            String output = telnetService.execute("show interfaces status err-disabled");
            return parser.parse(output);
        } catch (IOException e) {
            System.out.println("IOException on " + switchIp + ": " + e.getMessage());
            return Collections.emptyList();
        } finally {
            telnetService.disconnect();
        }
    }
}
