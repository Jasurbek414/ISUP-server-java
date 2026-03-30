package com.isup.repository;

import com.isup.entity.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    Page<EventLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<EventLog> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    List<EventLog> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT e FROM EventLog e JOIN FETCH e.project WHERE e.webhookStatus != 'delivered' AND e.webhookAttempts < :maxAttempts ORDER BY e.createdAt ASC")
    List<EventLog> findPendingWebhooks(int maxAttempts);

    @Query("SELECT COUNT(e) FROM EventLog e WHERE e.eventTime >= :since")
    long countSince(Instant since);

    @Query("SELECT e FROM EventLog e WHERE e.eventTime BETWEEN :from AND :to ORDER BY e.eventTime DESC")
    List<EventLog> findByTimeRange(Instant from, Instant to, Pageable pageable);

    @Query("SELECT e FROM EventLog e WHERE e.eventTime >= :start AND e.eventTime < :end ORDER BY e.eventTime ASC")
    List<EventLog> findByTimeRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT e FROM EventLog e WHERE e.employeeNo = :empNo AND e.eventTime >= :start AND e.eventTime < :end ORDER BY e.eventTime ASC")
    List<EventLog> findByEmployeeNoAndTimeRange(@Param("empNo") String employeeNo, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Flexible filtered query. Null parameters are treated as "no filter" via COALESCE pattern.
     * Filters: deviceId, eventType, employeeNo, startTime, endTime.
     */
    long countByWebhookStatus(String webhookStatus);

    long countByEventTimeBetween(Instant from, Instant to);

    @Query("SELECT e FROM EventLog e WHERE " +
           "(:deviceId   IS NULL OR e.deviceId   = :deviceId) AND " +
           "(:eventType  IS NULL OR e.eventType  = :eventType) AND " +
           "(:employeeNo IS NULL OR e.employeeNo = :employeeNo) AND " +
           "(:from       IS NULL OR e.eventTime >= :from) AND " +
           "(:to         IS NULL OR e.eventTime <= :to) " +
           "ORDER BY e.eventTime DESC")
    Page<EventLog> findFiltered(
            @Param("deviceId")   String  deviceId,
            @Param("eventType")  String  eventType,
            @Param("employeeNo") String  employeeNo,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable);
}
