package com.isup.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AttendanceEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;   // "attendance" | "motion" | "intrusion" | "line_crossing" | "face_detection"

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("device_model")
    private String deviceModel;

    @JsonProperty("employee_no")
    private String employeeNo;

    @JsonProperty("employee_name")
    private String employeeName;

    @JsonProperty("card_no")
    private String cardNo;

    @JsonProperty("verify_mode")
    private String verifyMode;  // face | card | fp | faceAndCard | ...

    @JsonProperty("direction")
    private String direction;   // in | out | break_in | break_out | overtime_in | overtime_out

    @JsonProperty("door_no")
    private Integer doorNo;

    @JsonProperty("channel")
    private Integer channel;

    @JsonProperty("event_time")
    private Instant eventTime;

    @JsonProperty("photo_base64")
    private String photoBase64;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("raw_payload")
    private String rawPayload;
}
