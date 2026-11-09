package ocs.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SciencePlan implements Serializable {

    private String planId;
    private List<ObservingProgram> programs;

    public SciencePlan(String planId, List<ObservingProgram> programs) {
        this.planId = planId;
        this.programs = new ArrayList<>(programs);
    }

    public String getPlanId() {
        return planId;
    }

    public ObservingProgram getProgram(String programId) {
        return programs.stream()
                .filter(program -> program.getProgramId().equals(programId))
                .findFirst()
                .orElse(null);
    }
}
