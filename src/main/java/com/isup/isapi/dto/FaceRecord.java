package com.isup.isapi.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceRecord {
    private String employeeNo;
    private String name;
    private String gender;       // "male" / "female"
    private String photoBase64;  // base64 encoded JPEG
}
