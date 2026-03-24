package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PTZ (Pan/Tilt/Zoom) camera control via ISAPI.
 */
public class PtzModule {
    private static final Logger log = LoggerFactory.getLogger(PtzModule.class);
    private final IsapiClient client;

    public PtzModule(IsapiClient client) {
        this.client = client;
    }

    /** PTZ continuous move - direction: LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT */
    public void continuousMove(int channelId, String direction, int speed) {
        String pan = "0", tilt = "0", zoom = "0";
        int s = Math.max(1, Math.min(speed, 100));
        switch (direction.toUpperCase()) {
            case "LEFT"     -> pan  = "-" + s;
            case "RIGHT"    -> pan  = "" + s;
            case "UP"       -> tilt = "" + s;
            case "DOWN"     -> tilt = "-" + s;
            case "ZOOM_IN"  -> zoom = "" + s;
            case "ZOOM_OUT" -> zoom = "-" + s;
        }
        String body = String.format(
            "{\"PTZData\":{\"pan\":%s,\"tilt\":%s,\"zoom\":%s}}",
            pan, tilt, zoom);
        try {
            client.put("/ISAPI/PTZCtrl/channels/" + channelId + "/continuous", body);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** Stop PTZ movement */
    public void stopMove(int channelId) {
        try {
            client.put("/ISAPI/PTZCtrl/channels/" + channelId + "/continuous",
                    "{\"PTZData\":{\"pan\":0,\"tilt\":0,\"zoom\":0}}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** Go to preset position */
    public void gotoPreset(int channelId, int presetId) {
        try {
            client.put("/ISAPI/PTZCtrl/channels/" + channelId + "/presets/" + presetId + "/goto", "{}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** Set preset position */
    public void setPreset(int channelId, int presetId, String presetName) {
        String body = String.format("{\"PTZPreset\":{\"id\":%d,\"presetName\":\"%s\",\"enabled\":true}}",
                presetId, presetName);
        try {
            client.put("/ISAPI/PTZCtrl/channels/" + channelId + "/presets/" + presetId, body);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** Get all presets */
    public String getPresets(int channelId) {
        try {
            return client.get("/ISAPI/PTZCtrl/channels/" + channelId + "/presets");
        } catch (IsapiException e) {
            log.debug("getPresets: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getPresets: {}", e.getMessage());
            return "{}";
        }
    }

    /** Absolute position move */
    public void absoluteMove(int channelId, int pan, int tilt, int zoom) {
        String body = String.format(
            "{\"PTZData\":{\"AbsoluteHigh\":{\"elevation\":%d,\"azimuth\":%d,\"absoluteZoom\":%d}}}",
            tilt, pan, zoom);
        try {
            client.put("/ISAPI/PTZCtrl/channels/" + channelId + "/absolute", body);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** Get current PTZ status */
    public String getStatus(int channelId) {
        try {
            return client.get("/ISAPI/PTZCtrl/channels/" + channelId + "/status");
        } catch (IsapiException e) {
            log.debug("getStatus: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getStatus: {}", e.getMessage());
            return "{}";
        }
    }
}
