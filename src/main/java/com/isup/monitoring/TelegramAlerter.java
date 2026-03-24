package com.isup.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class TelegramAlerter {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlerter.class);

    @Value("${isup.alert.telegram.token:}")
    private String token;

    @Value("${isup.alert.telegram.chat-id:}")
    private String chatId;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void send(String message) {
        if (token == null || token.isBlank()) return;
        if (chatId == null || chatId.isBlank()) return;

        try {
            String url  = "https://api.telegram.org/bot" + token + "/sendMessage";
            String body = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + escapeJson(message) + "\",\"parse_mode\":\"HTML\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Telegram alert failed: status={} body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Telegram alert error: {}", e.getMessage());
        }
    }

    public void alertServerStarted() {
        send("🟢 <b>ISUP Server Started</b>\nServer is up and accepting connections.");
    }

    public void alertServerStopped() {
        send("🔴 <b>ISUP Server Stopped</b>\nServer has been shut down.");
    }

    public void alertDeviceOffline(String deviceId, long offlineMinutes) {
        send("⚠️ <b>Device Offline</b>\nDevice <code>" + deviceId + "</code> has been offline for " + offlineMinutes + " minutes.");
    }

    public void alertWebhookFailed(Long projectId, int attempts) {
        send("❌ <b>Webhook Failed</b>\nProject ID <code>" + projectId + "</code> webhook failed after " + attempts + " attempts.");
    }

    public void alertDbDisconnected() {
        send("🔴 <b>Database Disconnected</b>\nThe ISUP server lost connection to the database. Events are being buffered.");
    }

    public void alertHighMemory(long percent) {
        send("⚠️ <b>High Memory Usage</b>\nJVM memory usage is at " + percent + "%.");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}
