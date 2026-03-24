package com.isup.maintenance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventCleanupService {

    private static final Logger log = LoggerFactory.getLogger(EventCleanupService.class);

    @Value("${isup.event.retention-days:90}")
    private int retentionDays;

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "0 0 2 * * *") // runs at 2 AM daily
    @Transactional
    public void cleanupOldEvents() {
        log.info("Starting event cleanup (retention={} days)...", retentionDays);
        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT cleanup_old_events(:retentionDays)");
            query.setParameter("retentionDays", retentionDays);
            Object result = query.getSingleResult();
            int deleted = result instanceof Number ? ((Number) result).intValue() : 0;
            log.info("Event cleanup complete: deleted {} old events", deleted);
        } catch (Exception e) {
            log.error("Event cleanup failed: {}", e.getMessage());
        }
    }
}
