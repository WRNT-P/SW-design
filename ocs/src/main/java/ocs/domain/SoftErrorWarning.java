package ocs.domain;

import java.io.Serializable;

public class SoftErrorWarning implements Serializable {

    private final String warningId;
    private final String condition;
    private WarningDecision decision;

    public SoftErrorWarning(String warningId, String condition) {
        this.warningId = warningId;
        this.condition = condition;
    }

    public String getWarningId() {
        return warningId;
    }

    public String getCondition() {
        return condition;
    }

    public WarningDecision getDecision() {
        return decision;
    }

    public boolean isResolved() {
        return decision != null;
    }

    public void resolve(WarningDecision decision) {
        this.decision = decision;
    }
}
