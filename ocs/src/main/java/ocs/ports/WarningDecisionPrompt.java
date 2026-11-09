package ocs.ports;

import ocs.domain.SoftErrorWarning;
import ocs.domain.WarningDecision;

public interface WarningDecisionPrompt {

    WarningDecision requestDecision(String programId, SoftErrorWarning warning);
}
