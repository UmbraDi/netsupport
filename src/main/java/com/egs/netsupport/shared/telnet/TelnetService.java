package com.egs.netsupport.shared.telnet;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TelnetService {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final TelnetClient telnet = new TelnetClient();
    private InputStream in;
    private OutputStream out;

    public void connect(String host, int port) throws IOException {
        telnet.setConnectTimeout(CONNECT_TIMEOUT_MS);
        telnet.setDefaultTimeout(READ_TIMEOUT_MS);
        telnet.connect(host, port);
        in = telnet.getInputStream();
        out = telnet.getOutputStream();
    }

    public void login(String login, String password) throws IOException {
        String promt = readUntil(":", 3000);
        if (promt == null) throw new IOException("No login prompt");

        sendLine(login);

        readUntil(":", 3000);
        sendLine(password);

        String shell = readUntilAny(new String[]{"#", ">"}, 5000);
        if (shell == null) throw new IOException("Login failed");
    }

    public String execute(String command) throws IOException {
        sendLine(command);
        String output = readUntilAny(new String[]{"#", ">"}, READ_TIMEOUT_MS);
        if (output == null) return "";
        return cleanOutput(output, command);
    }

    public void disconnect() {
        try {
            if (telnet.isConnected()) {
                telnet.disconnect();
            }
        } catch (IOException ignored) {}
    }

    private void sendLine(String line) throws IOException {
        out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String readUntil(String token, int timeoutMS) throws IOException {
        return readUntilAny(new String[]{token}, timeoutMS);
    }

    private String readUntilAny(String[] tokens, int timeoutMS) throws IOException {
        StringBuilder sb = new StringBuilder();
        long deadline = System.currentTimeMillis() + timeoutMS;
        byte[] buffer = new byte[1024];

        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) {
                int read = in.read(buffer);
                if (read > 0) {
                    sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                    for (String token: tokens) {
                        if (sb.toString().contains(token)) {
                            return sb.toString();
                        }
                    }
                }
            } else  {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private String cleanOutput(String raw, String command) {
        String result = raw;
        int echoIdx = result.indexOf(command);
        if (echoIdx >= 0) {
            result = result.substring(echoIdx + command.length());
        }
        int lastPromt = Math.max(result.lastIndexOf("#"), result.lastIndexOf(">"));
        if (lastPromt >= 0) {
            result = result.substring(0, lastPromt);
        }
        return result.trim();
    }
}
