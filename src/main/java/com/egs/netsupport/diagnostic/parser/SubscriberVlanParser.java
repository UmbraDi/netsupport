package com.egs.netsupport.diagnostic.parser;

/**
 * Ищет VLAN абонентского порта по выводу "show mac address-table".
 * Абонентский VLAN — всегда трёхзначный (вида 1xx/2xx/...).
 * MAC должен быть динамическим (DYNAMIC) — статические записи, CPU, Router, trunk-порты не учитываем.
 */
public class SubscriberVlanParser {

    // Пример строки (2500/C9300):
    //   175  0011.2233.4455  DYNAMIC  Gi1/0/12
    private static final String DYNAMIC = "DYNAMIC";

    // Порты, которые не являются абонентскими
    private static final String[] NON_SUBSCRIBER_PORT_PREFIXES = {
            "Po",      // port-channel
            "Twe",     // trunk etherchannel
            "Te",      // TenGigabit (uplink)
            "Fo",      // FortyGigabit
            "Hu",      // HundredGigabit
            "St",      // Stack
            "Null",
            "CPU",
            "Router"
    };

    /**
     * Возвращает первый найденный трёхзначный абонентский VLAN или null.
     */
    public Integer findSubscriberVlan(String output) {
        if (output == null || output.isBlank()) return null;

        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String[] parts = trimmed.split("\\s+");
            if (parts.length < 4) continue;

            String vlan = parts[0];
            String type = parts[2];
            String port = parts[3];

            if (!isThreeDigitVlan(vlan)) continue;
            if (!DYNAMIC.equalsIgnoreCase(type)) continue;
            if (isNonSubscriberPort(port)) continue;

            return Integer.parseInt(vlan);
        }
        return null;
    }

    private boolean isThreeDigitVlan(String vlan) {
        if (vlan == null || vlan.length() != 3) return false;
        for (char c : vlan.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private boolean isNonSubscriberPort(String port) {
        for (String prefix : NON_SUBSCRIBER_PORT_PREFIXES) {
            if (port.startsWith(prefix)) return true;
        }
        return false;
    }
}
