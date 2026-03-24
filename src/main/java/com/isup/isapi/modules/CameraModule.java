package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camera configuration and image settings via ISAPI.
 */
public class CameraModule {
    private static final Logger log = LoggerFactory.getLogger(CameraModule.class);
    private final IsapiClient client;

    public CameraModule(IsapiClient client) {
        this.client = client;
    }

    /** GET /ISAPI/Image/channels/{channelId} - image settings (brightness, contrast, etc) */
    public String getImageSettings(int channelId) {
        try {
            return client.get("/ISAPI/Image/channels/" + channelId);
        } catch (IsapiException e) {
            log.warn("getImageSettings ch{}: {}", channelId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("getImageSettings ch{}: {}", channelId, e.getMessage());
            throw new IsapiException(e.getMessage());
        }
    }

    /** PUT /ISAPI/Image/channels/{channelId} - update image settings */
    public void putImageSettings(int channelId, String jsonBody) {
        try {
            client.put("/ISAPI/Image/channels/" + channelId, jsonBody);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/System/Video/inputs/channels - list video input channels */
    public String getVideoInputChannels() {
        try {
            return client.get("/ISAPI/System/Video/inputs/channels");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/Streaming/channels - list streaming channels */
    public String getStreamingChannels() {
        try {
            return client.get("/ISAPI/Streaming/channels");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** PUT /ISAPI/Streaming/channels/{channelId} - update stream settings */
    public void putStreamingChannel(int channelId, String jsonBody) {
        try {
            client.put("/ISAPI/Streaming/channels/" + channelId, jsonBody);
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/System/Video/inputs/channels/{id}/overlays/text - OSD text */
    public String getOsdText(int channelId) {
        try {
            return client.get("/ISAPI/System/Video/inputs/channels/" + channelId + "/overlays/text");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/Streaming/channels/{channelId}/capabilities - stream capabilities */
    public String getStreamCapabilities(int channelId) {
        try {
            return client.get("/ISAPI/Streaming/channels/" + channelId + "/capabilities");
        } catch (IsapiException e) {
            log.debug("getStreamCapabilities: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getStreamCapabilities: {}", e.getMessage());
            return "{}";
        }
    }
}
