package com.isup.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.isup.entity.EventLog;
import com.isup.entity.Project;
import com.isup.event.AttendanceEvent;
import com.isup.monitoring.MetricsRegistry;
import com.isup.repository.EventLogRepository;
import com.isup.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    @Value("${isup.webhook.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${isup.webhook.read-timeout-ms:10000}")
    private int readTimeoutMs;

    private final ProjectRepository  projectRepo;
    private final EventLogRepository eventRepo;
    private final ObjectMapper       mapper;
    private final HttpClient         http;
    private final CircuitBreaker     circuitBreaker;
    private final MetricsRegistry    metrics;

    public WebhookDispatcher(ProjectRepository projectRepo,
                              EventLogRepository eventRepo,
                              CircuitBreaker circuitBreaker,
                              MetricsRegistry metrics) {
        this.projectRepo    = projectRepo;
        this.eventRepo      = eventRepo;
        this.circuitBreaker = circuitBreaker;
        this.metrics        = metrics;
        this.mapper         = new ObjectMapper().registerModule(new JavaTimeModule());
        this.http           = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();
    }

    public void dispatch(AttendanceEvent event, EventLog log_) {
        // If the event is already linked to a specific project (via device), only send to that project
        if (log_.getProject() != null) {
            Project project = log_.getProject();
            if (project.isActive() && project.getWebhookUrl() != null && !project.getWebhookUrl().isBlank()) {
                if (circuitBreaker.allowRequest(project.getId())) {
                    sendToProject(event, log_, project);
                } else {
                    log.debug("Circuit open for project {}, skipping webhook", project.getId());
                }
            }
            return;
        }

        // Fallback: if no project linked, we don't know where to send it (isolation)
        log.debug("Event {} has no project assigned, skipping webhook dispatch", log_.getId());
    }

    private void sendToProject(AttendanceEvent event, EventLog eventLog, Project project) {
        try {
            String body      = mapper.writeValueAsString(event);
            String signature = computeSignature(body, project.getSecretKey());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(project.getWebhookUrl()))
                    .timeout(Duration.ofMillis(readTimeoutMs))
                    .header("Content-Type", "application/json")
                    .header("X-ISUP-Signature", "sha256=" + signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            eventLog.setWebhookAttempts(eventLog.getWebhookAttempts() + 1);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                eventLog.setWebhookStatus("delivered");
                circuitBreaker.recordSuccess(project.getId());
                metrics.incrementWebhookSent();
                log.debug("Webhook delivered to project={} status={}", project.getId(), response.statusCode());
            } else {
                eventLog.setWebhookStatus("failed");
                circuitBreaker.recordFailure(project.getId());
                metrics.incrementWebhookFailed();
                log.warn("Webhook failed project={} status={}", project.getId(), response.statusCode());
            }
            eventRepo.save(eventLog);

        } catch (Exception e) {
            eventLog.setWebhookAttempts(eventLog.getWebhookAttempts() + 1);
            eventLog.setWebhookStatus("failed");
            eventRepo.save(eventLog);
            circuitBreaker.recordFailure(project.getId());
            metrics.incrementWebhookFailed();
            log.error("Webhook error project={} url={}: {}", project.getId(), project.getWebhookUrl(), e.getMessage());
        }
    }

    private String computeSignature(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
