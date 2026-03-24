package com.isup.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Parses SADP (Smart Active Discovery Protocol) UDP response packets.
 *
 * SADP response is an XML payload inside a binary envelope.
 * Many firmware versions return raw XML directly.
 */
public class SadpParser {

    private static final Logger log = LoggerFactory.getLogger(SadpParser.class);

    public record SadpInfo(String ip, String mac, String model, String serialNo,
                           String firmware, boolean activated, String deviceType) {}

    public static SadpInfo parse(byte[] data, String sourceIp) {
        try {
            String raw = new String(data, StandardCharsets.UTF_8);

            // Try XML first
            if (raw.contains("<Envelope") || raw.contains("<DeviceInfo") || raw.contains("<ProbeMatch")) {
                return parseXml(raw, sourceIp);
            }

            // Binary SADP header (16 bytes) + XML body
            if (data.length > 16) {
                String xmlPart = new String(data, 16, data.length - 16, StandardCharsets.UTF_8).trim();
                if (xmlPart.startsWith("<")) {
                    return parseXml(xmlPart, sourceIp);
                }
            }

            return new SadpInfo(sourceIp, null, null, null, null, false, "unknown");
        } catch (Exception e) {
            log.debug("SADP parse error from {}: {}", sourceIp, e.getMessage());
            return new SadpInfo(sourceIp, null, null, null, null, false, "unknown");
        }
    }

    private static SadpInfo parseXml(String xml, String sourceIp) {
        String ip         = extractTag(xml, "IPv4Address");
        if (ip == null) ip = sourceIp;
        String mac        = extractTag(xml, "MAC");
        String model      = extractTag(xml, "DeviceType");
        if (model == null) model = extractTag(xml, "Model");
        String serial     = extractTag(xml, "DeviceSN");
        if (serial == null) serial = extractTag(xml, "SerialNumber");
        String firmware   = extractTag(xml, "FirmwareVersion");
        String actStr     = extractTag(xml, "Activated");
        boolean activated = "true".equalsIgnoreCase(actStr);

        String deviceType = "unknown";
        if (model != null) {
            if (model.contains("K1T"))  deviceType = "face_terminal";
            else if (model.contains("2CD") || model.contains("2DE")) deviceType = "camera";
            else if (model.contains("K2"))  deviceType = "access_controller";
        }

        return new SadpInfo(ip, mac, model, serial, firmware, activated, deviceType);
    }

    private static String extractTag(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        if (start < 0) return null;
        int end = xml.indexOf("</" + tag + ">", start);
        if (end < 0) return null;
        return xml.substring(start + tag.length() + 2, end).trim();
    }
}
