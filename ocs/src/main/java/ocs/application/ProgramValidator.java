package ocs.application;

import ocs.ports.ObservingProgramRepository;

public class ProgramValidator {

    private final ObservingProgramRepository programRepo;
    private final WarningConfirmationService warningService;

    public ProgramValidator(ObservingProgramRepository programRepo, WarningConfirmationService warningService) {
        this.programRepo = programRepo;
        this.warningService = warningService;
    }

    public boolean validateProgram(String programId) {
        var program = programRepo.findById(programId);
        if (program == null) {
            return false;
        }
        var status = program.getStatus();
        var valid = program.validate();
        if (!"Submitted".equals(status) || !valid) {
            return false;
        }
        for (var warning : program.getWarnings()) {
            if (warning.isResolved()) {
                if (!warning.getDecision().isProceed()) {
                    return false;
                }
            } else if (!warningService.confirmWarning(programId, warning.getWarningId())) {
                return false;
            }
        }
        return true;
    }
}
