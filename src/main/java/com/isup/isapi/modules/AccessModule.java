package com.isup.isapi.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.isup.isapi.IsapiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AccessModule {
    private static final Logger log = LoggerFactory.getLogger(AccessModule.class);
    private final IsapiClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AccessModule(IsapiClient client) {
        this.client = client;
    }

    /** PUT /ISAPI/AccessControl/RemoteControl/door/{doorNo} with cmd=open */
    public boolean openDoor(int doorNo) {
        return sendDoorCommand(doorNo, "open");
    }

    /** PUT /ISAPI/AccessControl/RemoteControl/door/{doorNo} with cmd=close */
    public boolean closeDoor(int doorNo) {
        return sendDoorCommand(doorNo, "close");
    }

    /** Open door for X seconds, then close */
    public boolean openDoorTimed(int doorNo, int seconds) {
        boolean opened = openDoor(doorNo);
        if (opened) {
            scheduler.schedule(() -> sendDoorCommand(doorNo, "close"),
                    seconds, TimeUnit.SECONDS);
        }
        return opened;
    }

    /** Restore door to normal/auto mode */
    public boolean resumeDoor(int doorNo) {
        return sendDoorCommand(doorNo, "resume");
    }

    private boolean sendDoorCommand(int doorNo, String cmd) {
        try {
            ObjectNode inner = mapper.createObjectNode();
            inner.put("cmd", cmd);
            ObjectNode body = mapper.createObjectNode();
            body.set("RemoteControlDoor", inner);

            client.put("/ISAPI/AccessControl/RemoteControl/door/" + doorNo,
                    mapper.writeValueAsString(body));
            log.info("Door {} cmd={} on {}", doorNo, cmd, client.getBaseUrl());
            return true;
        } catch (Exception e) {
            log.error("Door {} cmd={} failed on {}: {}", doorNo, cmd, client.getBaseUrl(), e.getMessage());
            return false;
        }
    }
}
