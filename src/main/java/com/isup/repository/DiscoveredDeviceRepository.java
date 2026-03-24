package com.isup.repository;

import com.isup.entity.DiscoveredDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscoveredDeviceRepository extends JpaRepository<DiscoveredDevice, Long> {
    Optional<DiscoveredDevice> findByMac(String mac);
    Optional<DiscoveredDevice> findByIp(String ip);
    List<DiscoveredDevice> findAllByClaimedFalseOrderByLastSeenDesc();
}
