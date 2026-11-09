package ocs.application;

import ocs.ports.AuditRepository;
import ocs.ports.ObservingProgramRepository;
import ocs.ports.WarningDecisionPrompt;

public class WarningConfirmationService {

    private final ObservingProgramRepository programRepo;
    private final AuditRepository auditRepo;
    private final WarningDecisionPrompt decisionPrompt;

    public WarningConfirmationService(ObservingProgramRepository programRepo, AuditRepository auditRepo,
            WarningDecisionPrompt decisionPrompt) {
        this.programRepo = programRepo;
        this.auditRepo = auditRepo;
        this.decisionPrompt = decisionPrompt;
    }

    public boolean confirmWarning(String programId, String warningId) {
        var program = programRepo.findById(programId);
        if (program == null) {
            return false;
        }
        var warning = program.getWarning(warningId);
        if (warning == null || warning.isResolved()) {
            return false;
        }
        var decision = decisionPrompt.requestDecision(programId, warning);
        if (decision == null) {
            return false;
        }
        var outcome = decision.isProceed() ? "PROCEED" : "STOP";
        warning.resolve(decision);
        auditRepo.logAction("Warning " + warningId + " decisionId=" + decision.getDecisionId()
                + " decision=" + outcome + " by=" + decision.getDecidedBy()
                + " at=" + decision.getDecidedAt());
        programRepo.save(program);
        return decision.isProceed();
    }
}
