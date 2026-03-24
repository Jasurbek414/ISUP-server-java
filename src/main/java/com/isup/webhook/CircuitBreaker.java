package com.isup.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private static final int    FAILURE_THRESHOLD      = 5;
    private static final long   OPEN_DURATION_MS       = 5 * 60 * 1000L; // 5 minutes

    enum State { CLOSED, OPEN, HALF_OPEN }

    static class CircuitState {
        volatile State state           = State.CLOSED;
        volatile int   failureCount    = 0;
        volatile long  openedAt        = 0;

        synchronized boolean allowRequest() {
            switch (state) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (System.currentTimeMillis() - openedAt >= OPEN_DURATION_MS) {
                        state = State.HALF_OPEN;
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    return true;
                default:
                    return false;
            }
        }

        synchronized void recordSuccess() {
            failureCount = 0;
            state        = State.CLOSED;
        }

        synchronized void recordFailure() {
            failureCount++;
            if (state == State.HALF_OPEN) {
                state    = State.OPEN;
                openedAt = System.currentTimeMillis();
            } else if (state == State.CLOSED && failureCount >= FAILURE_THRESHOLD) {
                state    = State.OPEN;
                openedAt = System.currentTimeMillis();
            }
        }
    }

    private final ConcurrentHashMap<Long, CircuitState> circuits = new ConcurrentHashMap<>();

    private CircuitState getOrCreate(Long projectId) {
        return circuits.computeIfAbsent(projectId, k -> new CircuitState());
    }

    public boolean allowRequest(Long projectId) {
        return getOrCreate(projectId).allowRequest();
    }

    public void recordSuccess(Long projectId) {
        CircuitState cs = getOrCreate(projectId);
        State before    = cs.state;
        cs.recordSuccess();
        if (before != State.CLOSED) {
            log.info("Circuit CLOSED for project={}", projectId);
        }
    }

    public void recordFailure(Long projectId) {
        CircuitState cs = getOrCreate(projectId);
        cs.recordFailure();
        if (cs.state == State.OPEN) {
            log.warn("Circuit OPEN for project={} after {} failures", projectId, cs.failureCount);
        }
    }
}
