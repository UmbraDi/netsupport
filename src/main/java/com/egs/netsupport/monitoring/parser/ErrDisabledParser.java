package com.egs.netsupport.monitoring.parser;

import com.egs.netsupport.monitoring.model.PortError;

import java.util.ArrayList;
import java.util.List;

public class ErrDisabledParser {
    public List<PortError> parse(String output) {
        List<PortError> result = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return result;
        }

        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("Port")) continue; // заголовок

            // ищем строки со статусом err-disabled
            if (trimmed.toLowerCase().contains("err-disabled")) {
                String[] parts = trimmed.split("\\s+");
                // Формат: Port [Name] err-disabled Reason
                // Name может отсутствовать, поэтому ищем индекс "err-disabled"
                int idx = -1;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equalsIgnoreCase("err-disabled")) {
                        idx = i;
                        break;
                    }
                }
                if (idx > 0) {
                    String port = parts[0];
                    String reason = (idx + 1 < parts.length) ? parts[idx + 1] : "unknown";
                    result.add(new PortError(port, reason));
                }
            }
        }
        return result;
    }

}
