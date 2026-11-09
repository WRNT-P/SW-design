package ocs.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObservingProgram implements Serializable {

    private final String programId;
    private String status;
    private final InstrumentConfiguration configuration;
    private final List<SoftErrorWarning> warnings = new ArrayList<>();
    private final List<ObservationSession> sessions = new ArrayList<>();
    private boolean isLocked;

    public ObservingProgram(String programId, InstrumentConfiguration configuration) {
        this.programId = programId;
        this.configuration = configuration;
    }

    public String getProgramId() {
        return programId;
    }

    public boolean validate() {
        return configuration != null;
    }

    public void submit() {
        status = "Submitted";
    }

    public InstrumentConfiguration getConfiguration() {
        return configuration;
    }

    public String getStatus() {
        return status;
    }

    public List<SoftErrorWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public SoftErrorWarning getWarning(String warningId) {
        return warnings.stream().filter(warning -> warning.getWarningId().equals(warningId))
                .findFirst().orElse(null);
    }

    public void addWarning(SoftErrorWarning warning) {
        warnings.add(warning);
    }

    public void lock() {
        isLocked = true;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void start() {
        status = "Running";
        isLocked = true;
        ObservationSession session = new ObservationSession(UUID.randomUUID().toString());
        session.start();
        sessions.add(session);
    }
}
