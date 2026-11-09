package ocs.application;

import ocs.ports.ObservingProgramRepository;

public class ProgramValidator {

    private ObservingProgramRepository programRepo;

    public ProgramValidator(ObservingProgramRepository programRepo) {
        this.programRepo = programRepo;
    }

    public boolean validateProgram(String programId, WarningConfirmationService warningService) {
        throw new UnsupportedOperationException();
    }
}
