package com.isup.isapi.dto;

import lombok.Data;

@Data
public class DeviceInfo {
    private String deviceName;
    private String deviceID;
    private String model;
    private String serialNumber;
    private String firmwareVersion;
    private String macAddress;
    private String deviceType;
}
