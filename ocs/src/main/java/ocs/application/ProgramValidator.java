package ocs.application;

import ocs.ports.ObservingProgramRepository;

public class ProgramValidator {

    private final ObservingProgramRepository programRepo;

    public ProgramValidator(ObservingProgramRepository programRepo) {
        this.programRepo = programRepo;
    }

    public boolean validateProgram(String programId, WarningConfirmationService warningService) {
        var program = programRepo.findById(programId);
        if (program == null || !"Submitted".equals(program.getStatus()) || !program.validate()) {
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
