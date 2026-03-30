package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device system management: time, reboot, network config.
 */
public class SystemModule {
    private static final Logger log = LoggerFactory.getLogger(SystemModule.class);
    private final IsapiClient client;

    public SystemModule(IsapiClient client) {
        this.client = client;
    }

    /** GET /ISAPI/System/time - device time */
    public String getTime() {
        try {
            return client.get("/ISAPI/System/time?format=json");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** PUT /ISAPI/System/time - sync device time */
    public void syncTime(String isoDateTime, String timezone) {
        String body = String.format(
            "{\"Time\":{\"timeMode\":\"NTP\",\"localTime\":\"%s\",\"timeZone\":\"%s\"}}",
            isoDateTime, timezone);
        try {
            client.put("/ISAPI/System/time", body);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** POST /ISAPI/System/reboot - reboot device */
    public void reboot() {
        try {
            client.post("/ISAPI/System/reboot", "{}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/System/Network/interfaces - network settings */
    public String getNetworkInterfaces() {
        try {
            return client.get("/ISAPI/System/Network/interfaces?format=json");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/System/Network/interfaces/1/ipAddress - IP config */
    public String getIpConfig() {
        try {
            return client.get("/ISAPI/System/Network/interfaces/1/ipAddress?format=json");
        } catch (IsapiException e) {
            log.debug("getIpConfig: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getIpConfig: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/System/capabilities - device full capabilities */
    public String getCapabilities() {
        try {
            return client.get("/ISAPI/System/capabilities?format=json");
        } catch (IsapiException e) {
            log.debug("getCapabilities: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getCapabilities: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/System/status - system status (CPU, memory, temp) */
    public String getSystemStatus() {
        try {
            return client.get("/ISAPI/System/status?format=json");
        } catch (IsapiException e) {
            log.debug("getSystemStatus: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getSystemStatus: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/System/Storage/hdd - storage info */
    public String getStorageInfo() {
        try {
            return client.get("/ISAPI/System/Storage/hdd?format=json");
        } catch (IsapiException e) {
            log.debug("getStorageInfo: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getStorageInfo: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/System/deviceInfo - basic device info (model, firmware, serial) */
    public String getDeviceInfo() {
        try {
            return client.get("/ISAPI/System/deviceInfo?format=json");
        } catch (IsapiException e) {
            log.debug("getDeviceInfo: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getDeviceInfo: {}", e.getMessage());
            return "{}";
        }
    }
}
