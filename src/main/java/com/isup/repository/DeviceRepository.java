package com.isup.repository;

import com.isup.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findAllByProjectId(Long projectId);

    @Modifying
    @Query("UPDATE Device d SET d.status = :status, d.lastSeen = :ts, d.ipAddress = :ip WHERE d.deviceId = :deviceId")
    void updateStatus(String deviceId, String status, Instant ts, String ip);

    @Query("SELECT d FROM Device d WHERE d.status = 'online'")
    List<Device> findOnlineDevices();

    @Modifying
    @Query("UPDATE Device d SET d.model = COALESCE(:model, d.model), d.firmware = COALESCE(:firmware, d.firmware), d.capabilities = :capabilities WHERE d.deviceId = :deviceId")
    void updateCapabilities(String deviceId, String model, String firmware, String capabilities);
}
