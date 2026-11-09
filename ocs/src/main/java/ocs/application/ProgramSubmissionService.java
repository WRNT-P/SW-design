package ocs.application;

import ocs.domain.ObservingProgram;
import ocs.domain.SciencePlan;
import ocs.ports.ObservingProgramRepository;
import ocs.ports.SciencePlanRepository;

public class ProgramSubmissionService {

    private SciencePlanRepository sciencePlanRepo;
    private ObservingProgramRepository programRepo;

    public ProgramSubmissionService(SciencePlanRepository sciencePlanRepo, ObservingProgramRepository programRepo) {
        this.sciencePlanRepo = sciencePlanRepo;
        this.programRepo = programRepo;
    }

    public boolean submitProgram(String planId, String programId) {
        SciencePlan plan = sciencePlanRepo.findById(planId);
        if (plan == null) {
            return false;
        }
        ObservingProgram program = plan.getProgram(programId);
        if (program == null) {
            return false;
        }
        if (program.validate()) {
            program.submit();
            programRepo.save(program);
            return true;
        }
        return false;
    }
}
