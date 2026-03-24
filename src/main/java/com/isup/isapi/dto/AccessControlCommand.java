package com.isup.isapi.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessControlCommand {
    private String deviceId;
    private int doorNo;
    private String command;  // "open" / "close" / "resume"
    private Integer timedSeconds; // for open-timed
}
