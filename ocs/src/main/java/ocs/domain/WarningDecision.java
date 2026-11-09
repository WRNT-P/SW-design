package ocs.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public class WarningDecision implements Serializable {

    private final String decisionId;
    private final String outcome;
    private final String decidedBy;
    private final LocalDateTime decidedAt;

    public WarningDecision(String decisionId, String outcome, String decidedBy, LocalDateTime decidedAt) {
        this.decisionId = decisionId;
        this.outcome = outcome;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public boolean isProceed() {
        return "PROCEED".equals(outcome);
    }

    public String getDecisionId() {
        return decisionId;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }
}
