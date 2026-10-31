package com.egs.netsupport.diagnostic.parser;

import com.egs.netsupport.diagnostic.model.FdbEntry;

import java.util.ArrayList;
import java.util.List;

public class FdbParser {

    public List<FdbEntry> findAll(String output, String last4MacChars) {
        List<FdbEntry> result = new ArrayList<>();
        if (output == null || output.isBlank()) return result;

        String search = last4MacChars.toLowerCase();

        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.toLowerCase().contains(search)) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 4) {
                    result.add(new FdbEntry(
                            parts[0],           // VLAN
                            parts[1],           // MAC
                            parts[2],           // TYPE
                            parts[3]            // PORT
                    ));
                }
            }
        }
        return result;
    }
}
