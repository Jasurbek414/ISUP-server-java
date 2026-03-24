package com.isup.lifecycle;

import com.isup.protocol.IsupProtocol;
import com.isup.session.DeviceSession;
import com.isup.session.SessionRegistry;
import com.isup.webhook.WebhookRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GracefulShutdown implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdown.class);

    private static final long SHUTDOWN_WAIT_MS = 30_000L;

    private final SessionRegistry    sessionRegistry;
    private final WebhookRetryService webhookRetryService;

    public GracefulShutdown(SessionRegistry sessionRegistry,
                             WebhookRetryService webhookRetryService) {
        this.sessionRegistry    = sessionRegistry;
        this.webhookRetryService = webhookRetryService;
    }

    @Override
    public void destroy() {
        log.info("Graceful shutdown initiated...");

        // Step 1: Send LOGOUT packet to all connected devices
        List<DeviceSession> sessions = sessionRegistry.getAllSessions();
        log.info("Sending LOGOUT to {} connected devices", sessions.size());
        for (DeviceSession session : sessions) {
            try {
                if (session.isActive()) {
                    // Build a logout packet using session id
                    io.netty.buffer.ByteBuf logoutPacket =
                            IsupProtocol.buildLogoutPacket(session.getSessionId());
                    session.getChannel().writeAndFlush(logoutPacket);
                    log.debug("Sent LOGOUT to device={}", session.getDeviceId());
                }
            } catch (Exception e) {
                log.warn("Failed to send LOGOUT to device={}: {}", session.getDeviceId(), e.getMessage());
            }
        }

        // Step 2: Wait up to 30s for pending webhooks to complete
        long deadline = System.currentTimeMillis() + SHUTDOWN_WAIT_MS;
        log.info("Waiting up to {}s for pending webhooks...", SHUTDOWN_WAIT_MS / 1000);
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("Graceful shutdown complete");
    }
}
