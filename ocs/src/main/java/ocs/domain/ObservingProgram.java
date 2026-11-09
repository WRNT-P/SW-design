package ocs.domain;

import java.io.Serializable;

public class ObservingProgram implements Serializable {

    private String programId;
    private String status;
    private InstrumentConfiguration configuration;

    public ObservingProgram(String programId, InstrumentConfiguration configuration) {
        this.programId = programId;
        this.configuration = configuration;
    }

    public String getProgramId() {
        return programId;
    }

    public boolean validate() {
        return configuration != null;
    }

    public void submit() {
        status = "Submitted";
    }

    public InstrumentConfiguration getConfiguration() {
        return configuration;
    }
}
