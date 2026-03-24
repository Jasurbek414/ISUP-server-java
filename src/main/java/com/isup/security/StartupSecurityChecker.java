package com.isup.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupSecurityChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupSecurityChecker.class);

    @Value("${isup.admin.secret:changeme123}")
    private String adminSecret;

    @Value("${isup.alert.telegram.token:}")
    private String telegramToken;

    @Override
    public void run(ApplicationArguments args) {
        if ("changeme123".equals(adminSecret)) {
            log.warn("⚠️ ADMIN_SECRET is default! Change it in .env before production use.");
        }
        if (telegramToken == null || telegramToken.isBlank()) {
            log.info("Telegram alerting disabled (ALERT_TELEGRAM_TOKEN not set)");
        }
    }
}
