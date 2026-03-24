package com.isup.isapi.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isup.isapi.IsapiClient;
import com.isup.isapi.dto.DeviceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceModule {
    private static final Logger log = LoggerFactory.getLogger(DeviceModule.class);
    private final IsapiClient client;
    private final ObjectMapper mapper;

    public DeviceModule(IsapiClient client) {
        this.client = client;
        this.mapper = new ObjectMapper();
    }

    /**
     * GET /ISAPI/System/deviceInfo
     * Returns device model, serialNo, firmwareVersion, macAddress.
     */
    public DeviceInfo getDeviceInfo() {
        try {
            String response = client.get("/ISAPI/System/deviceInfo");
            DeviceInfo info = new DeviceInfo();

            // Try JSON parse first, then XML
            if (response.trim().startsWith("{")) {
                JsonNode node = mapper.readTree(response);
                JsonNode di = node.has("DeviceInfo") ? node.get("DeviceInfo") : node;
                info.setDeviceName(getText(di, "deviceName"));
                info.setDeviceID(getText(di, "deviceID"));
                info.setModel(getText(di, "model"));
                info.setSerialNumber(getText(di, "serialNumber"));
                info.setFirmwareVersion(getText(di, "firmwareReleasedDate", "firmwareVersion"));
                info.setMacAddress(getText(di, "macAddress"));
                info.setDeviceType(getText(di, "deviceType"));
            } else {
                // XML: extract with simple regex
                info.setDeviceName(extractXml(response, "deviceName"));
                info.setModel(extractXml(response, "model"));
                info.setSerialNumber(extractXml(response, "serialNumber"));
                info.setFirmwareVersion(extractXml(response, "firmwareReleasedDate"));
                info.setMacAddress(extractXml(response, "macAddress"));
                info.setDeviceType(extractXml(response, "deviceType"));
            }
            return info;
        } catch (Exception e) {
            log.warn("getDeviceInfo failed: {}", e.getMessage());
            return null;
        }
    }

    private String getText(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) return node.get(key).asText();
        }
        return null;
    }

    private String extractXml(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        int end   = xml.indexOf("</" + tag + ">");
        if (start == -1 || end == -1) return null;
        return xml.substring(start + tag.length() + 2, end).trim();
    }
}
