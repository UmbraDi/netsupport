package com.egs.netsupport.diagnostic.parser;

public class FdbParser {

    public String findPort(String output, String last4MacChars) {
        if (output == null || output.isBlank()) return null;

        String search = last4MacChars.toLowerCase();

        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // ищем строку, где есть последние 4 символа MAC (без учёта регистра)
            if (trimmed.toLowerCase().contains(search)) {
                // порт — последний столбец
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 4) {
                    return parts[parts.length - 1];
                }
            }
        }
        return null;
    }
}
