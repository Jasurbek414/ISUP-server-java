package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alarm input/output and event notifications configuration.
 */
public class AlarmModule {
    private static final Logger log = LoggerFactory.getLogger(AlarmModule.class);
    private final IsapiClient client;

    public AlarmModule(IsapiClient client) {
        this.client = client;
    }

    /** GET /ISAPI/System/IO/inputs - alarm inputs status */
    public String getAlarmInputs() {
        try {
            return client.get("/ISAPI/System/IO/inputs");
        } catch (IsapiException e) {
            log.debug("getAlarmInputs: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getAlarmInputs: {}", e.getMessage());
            return "{}";
        }
    }

    /** PUT /ISAPI/System/IO/outputs/{id}/trigger - trigger alarm output */
    public void triggerAlarmOutput(int outputId) {
        try {
            client.put("/ISAPI/System/IO/outputs/" + outputId + "/trigger",
                    "{\"IOPortData\":{\"outputState\":\"high\"}}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** PUT /ISAPI/System/IO/outputs/{id}/trigger - clear alarm output */
    public void clearAlarmOutput(int outputId) {
        try {
            client.put("/ISAPI/System/IO/outputs/" + outputId + "/trigger",
                    "{\"IOPortData\":{\"outputState\":\"low\"}}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/Event/triggers - event triggers config */
    public String getEventTriggers() {
        try {
            return client.get("/ISAPI/Event/triggers");
        } catch (IsapiException e) {
            log.debug("getEventTriggers: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getEventTriggers: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/Smart/LineDetection/channels/{id} - line crossing config */
    public String getLineCrossingConfig(int channelId) {
        try {
            return client.get("/ISAPI/Smart/LineDetection/channels/" + channelId);
        } catch (IsapiException e) {
            log.debug("getLineCrossingConfig: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getLineCrossingConfig: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/Smart/FieldDetection/channels/{id} - intrusion detection config */
    public String getIntrusionConfig(int channelId) {
        try {
            return client.get("/ISAPI/Smart/FieldDetection/channels/" + channelId);
        } catch (IsapiException e) {
            log.debug("getIntrusionConfig: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getIntrusionConfig: {}", e.getMessage());
            return "{}";
        }
    }

    /** GET /ISAPI/Smart/MotionDetection/channels/{id} - motion detection */
    public String getMotionDetection(int channelId) {
        try {
            return client.get("/ISAPI/Smart/MotionDetection/channels/" + channelId);
        } catch (IsapiException e) {
            log.debug("getMotionDetection: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getMotionDetection: {}", e.getMessage());
            return "{}";
        }
    }
}
