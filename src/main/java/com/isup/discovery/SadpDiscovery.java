package com.isup.discovery;

import com.isup.entity.DiscoveredDevice;
import com.isup.repository.DiscoveredDeviceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SadpDiscovery {

    private static final Logger log      = LoggerFactory.getLogger(SadpDiscovery.class);
    private static final String BROADCAST = "255.255.255.255";
    private static final int    SADP_PORT = 37020;

    // SADP broadcast probe packet (Hikvision SADP v3)
    private static final byte[] PROBE_PACKET = buildProbe();

    @Value("${isup.sadp.enabled:true}")
    private boolean enabled;

    @Value("${isup.sadp.port:37020}")
    private int port;

    private final DiscoveredDeviceRepository repo;

    private DatagramSocket   socket;
    private Thread           listenerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SadpDiscovery(DiscoveredDeviceRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void start() {
        if (!enabled) return;
        try {
            socket = new DatagramSocket(port);
            socket.setBroadcast(true);
            socket.setSoTimeout(0);
            running.set(true);

            listenerThread = new Thread(this::listen, "sadp-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            log.info("SADP discovery listening on UDP port {}", port);
            // Initial scan after startup
            Thread scanThread = new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                broadcast();
            }, "sadp-initial-scan");
            scanThread.setDaemon(true);
            scanThread.start();
        } catch (Exception e) {
            log.warn("SADP discovery could not start: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) socket.close();
    }

    @Scheduled(fixedDelay = 60_000)
    public void scheduledScan() {
        if (enabled) broadcast();
    }

    public void broadcast() {
        if (socket == null || socket.isClosed()) return;
        try {
            DatagramPacket pkt = new DatagramPacket(
                PROBE_PACKET, PROBE_PACKET.length,
                InetAddress.getByName(BROADCAST), SADP_PORT
            );
            socket.send(pkt);
            log.debug("SADP broadcast sent");
        } catch (Exception e) {
            log.debug("SADP broadcast error: {}", e.getMessage());
        }
    }

    private void listen() {
        byte[] buf = new byte[4096];
        while (running.get() && !socket.isClosed()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                byte[] data = Arrays.copyOf(pkt.getData(), pkt.getLength());
                String ip   = pkt.getAddress().getHostAddress();
                handleResponse(data, ip);
            } catch (SocketException e) {
                if (running.get()) log.debug("SADP socket closed");
                break;
            } catch (Exception e) {
                log.debug("SADP receive error: {}", e.getMessage());
            }
        }
    }

    private void handleResponse(byte[] data, String sourceIp) {
        SadpParser.SadpInfo info = SadpParser.parse(data, sourceIp);
        log.info("SADP discovered: ip={} mac={} model={}", info.ip(), info.mac(), info.model());

        Optional<DiscoveredDevice> existing = info.mac() != null
            ? repo.findByMac(info.mac())
            : repo.findByIp(info.ip());

        if (existing.isPresent()) {
            DiscoveredDevice d = existing.get();
            d.setIp(info.ip());
            d.setLastSeen(Instant.now());
            if (info.firmware() != null) d.setFirmware(info.firmware());
            d.setActivated(info.activated());
            repo.save(d);
        } else {
            DiscoveredDevice d = DiscoveredDevice.builder()
                .ip(info.ip())
                .mac(info.mac())
                .model(info.model())
                .deviceType(info.deviceType())
                .serialNo(info.serialNo())
                .firmware(info.firmware())
                .activated(info.activated())
                .build();
            repo.save(d);
        }
    }

    private static byte[] buildProbe() {
        // Standard SADP probe: 20-byte header
        byte[] probe = new byte[20];
        probe[0]  = 0x20;  // version
        probe[1]  = 0x00;
        probe[2]  = 0x00;
        probe[3]  = 0x00;
        probe[4]  = 0x00;  // message type: probe
        probe[5]  = 0x00;
        probe[6]  = 0x00;
        probe[7]  = 0x00;
        // remaining bytes: sequence, reserved
        return probe;
    }
}
