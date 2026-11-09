package ocs.domain;

import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;

public class ObservationSession implements Serializable {

    private final String sessionId;
    private String status;
    private LocalDateTime startTime;

    public ObservationSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public void start() {
        status = "Running";
        startTime = LocalDateTime.now(Clock.systemDefaultZone());
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
}
