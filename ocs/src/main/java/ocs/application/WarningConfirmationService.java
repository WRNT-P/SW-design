package ocs.application;

import ocs.ports.AuditRepository;
import ocs.ports.ObservingProgramRepository;

public class WarningConfirmationService {

    private ObservingProgramRepository programRepo;
    private AuditRepository auditRepo;

    public WarningConfirmationService(ObservingProgramRepository programRepo, AuditRepository auditRepo) {
        this.programRepo = programRepo;
        this.auditRepo = auditRepo;
    }

    public boolean confirmWarning(String programId, String warningId, String observerId, boolean proceed) {
        throw new UnsupportedOperationException();
    }
}
