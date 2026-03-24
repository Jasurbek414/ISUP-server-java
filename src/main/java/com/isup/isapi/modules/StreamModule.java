package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamModule {
    private static final Logger log = LoggerFactory.getLogger(StreamModule.class);
    private final IsapiClient client;
    private final String deviceIp;
    private final String username;
    private final String password;

    public StreamModule(IsapiClient client, String deviceIp, String username, String password) {
        this.client   = client;
        this.deviceIp = deviceIp;
        this.username = username;
        this.password = password;
    }

    /** Main stream RTSP URL (high quality). */
    public String getMainStreamUrl() {
        return buildRtspUrl(101);
    }

    /** Sub stream RTSP URL (lower quality / faster). */
    public String getSubStreamUrl() {
        return buildRtspUrl(102);
    }

    /** Channel-based RTSP URL format (alternative). */
    public String getChannelUrl(int channel) {
        return "rtsp://" + encode(username) + ":" + encode(password) + "@" + deviceIp + ":554/ch" + channel + "/main/av_stream";
    }

    private String buildRtspUrl(int channel) {
        return "rtsp://" + encode(username) + ":" + encode(password) + "@" + deviceIp + ":554/Streaming/Channels/" + channel;
    }

    private String encode(String s) {
        return s == null ? "" : s.replace("@", "%40").replace(":", "%3A");
    }
}
