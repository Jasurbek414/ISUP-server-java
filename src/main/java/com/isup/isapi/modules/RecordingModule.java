package com.isup.isapi.modules;

import com.isup.isapi.IsapiClient;
import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recording control and playback for NVR/DVR devices.
 */
public class RecordingModule {
    private static final Logger log = LoggerFactory.getLogger(RecordingModule.class);
    private final IsapiClient client;

    public RecordingModule(IsapiClient client) {
        this.client = client;
    }

    /** GET /ISAPI/ContentMgmt/record/control/manual/tracks - manual recording channels */
    public String getRecordingStatus() {
        try {
            return client.get("/ISAPI/ContentMgmt/record/control/manual/tracks");
        } catch (IsapiException e) {
            log.debug("getRecordingStatus: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getRecordingStatus: {}", e.getMessage());
            return "{}";
        }
    }

    /** POST /ISAPI/ContentMgmt/record/control/manual/tracks/{channelId}/start */
    public void startRecording(int channelId) {
        try {
            client.put("/ISAPI/ContentMgmt/record/control/manual/tracks/" + channelId + "/start", "{}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** POST /ISAPI/ContentMgmt/record/control/manual/tracks/{channelId}/stop */
    public void stopRecording(int channelId) {
        try {
            client.put("/ISAPI/ContentMgmt/record/control/manual/tracks/" + channelId + "/stop", "{}");
        } catch (IsapiException e) {
            throw e;
        } catch (Exception e) {
            throw new IsapiException(e.getMessage());
        }
    }

    /** GET /ISAPI/ContentMgmt/record/config - recording schedule */
    public String getRecordingSchedule() {
        try {
            return client.get("/ISAPI/ContentMgmt/record/config");
        } catch (IsapiException e) {
            log.debug("getRecordingSchedule: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("getRecordingSchedule: {}", e.getMessage());
            return "{}";
        }
    }

    /** Search recordings: POST /ISAPI/ContentMgmt/search */
    public String searchRecordings(String channelId, String startTime, String endTime) {
        String body = String.format(
            "{\"CMSearchDescription\":{\"searchID\":\"1\",\"trackList\":[{\"id\":\"%s\"}]," +
            "\"timeSpanList\":[{\"startTime\":\"%s\",\"endTime\":\"%s\"}]," +
            "\"maxResults\":40,\"searchResultPostion\":0,\"metadataList\":[{\"type\":\"ISAPI://Streaming/tracks\"}]}}",
            channelId, startTime, endTime);
        try {
            return client.post("/ISAPI/ContentMgmt/search", body);
        } catch (IsapiException e) {
            log.debug("searchRecordings: {}", e.getMessage());
            return "{}";
        } catch (Exception e) {
            log.debug("searchRecordings: {}", e.getMessage());
            return "{}";
        }
    }
}
