package com.isup.isapi.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.isup.isapi.IsapiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlacklistModule {
    private static final Logger log = LoggerFactory.getLogger(BlacklistModule.class);
    private final IsapiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public BlacklistModule(IsapiClient client) {
        this.client = client;
    }

    /**
     * Add employee to blacklist on the device.
     * POST /ISAPI/AccessControl/UserInfo/SetUp
     * Sets userType=blackList (which disables access).
     */
    public boolean addToBlacklist(String employeeNo, String name) {
        try {
            ObjectNode valid = mapper.createObjectNode();
            valid.put("enable", false);
            valid.put("beginTime", "2000-01-01T00:00:00");
            valid.put("endTime", "2000-01-01T00:00:00");

            ObjectNode user = mapper.createObjectNode();
            user.put("employeeNo", employeeNo);
            user.put("name", name != null ? name : employeeNo);
            user.put("userType", "blackList");
            user.set("Valid", valid);

            ObjectNode body = mapper.createObjectNode();
            body.putArray("UserInfo").add(user);

            client.post("/ISAPI/AccessControl/UserInfo/SetUp",
                    mapper.writeValueAsString(body));
            log.info("addToBlacklist employee={} on {}", employeeNo, client.getBaseUrl());
            return true;
        } catch (Exception e) {
            log.error("addToBlacklist {} failed: {}", employeeNo, e.getMessage());
            return false;
        }
    }

    /**
     * Remove from blacklist — restore normal access.
     */
    public boolean removeFromBlacklist(String employeeNo) {
        try {
            ObjectNode valid = mapper.createObjectNode();
            valid.put("enable", true);
            valid.put("beginTime", "2000-01-01T00:00:00");
            valid.put("endTime", "2037-12-31T23:59:59");

            ObjectNode user = mapper.createObjectNode();
            user.put("employeeNo", employeeNo);
            user.put("userType", "normal");
            user.set("Valid", valid);

            ObjectNode body = mapper.createObjectNode();
            body.putArray("UserInfo").add(user);

            client.post("/ISAPI/AccessControl/UserInfo/SetUp",
                    mapper.writeValueAsString(body));
            log.info("removeFromBlacklist employee={} on {}", employeeNo, client.getBaseUrl());
            return true;
        } catch (Exception e) {
            log.error("removeFromBlacklist {} failed: {}", employeeNo, e.getMessage());
            return false;
        }
    }
}
