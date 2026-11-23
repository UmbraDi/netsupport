package com.egs.netsupport.monitoring;

import com.egs.netsupport.monitoring.model.PortError;
import com.egs.netsupport.monitoring.parser.ErrDisabledParser;
import com.egs.netsupport.shared.model.Credentials;
import com.egs.netsupport.shared.telnet.TelnetService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class PortStatusService {

    private final ErrDisabledParser parser = new ErrDisabledParser();

    public List<PortError> getErrDisabledPorts(String switchIp, Credentials creds) {
        TelnetService telnet = new TelnetService();
        try {
            telnet.connect(switchIp, 23);
            telnet.login(creds.login, creds.password);
            telnet.execute("terminal length 0");

            String output = telnet.execute("show interfaces status err-disabled");
            return parser.parse(output);
        } catch (IOException e) {
            System.out.println("IOException on " + switchIp + ": " + e.getMessage());
            return Collections.emptyList();
        } finally {
            telnet.disconnect();
        }
    }
}
