package ocs.application;

import ocs.domain.InstrumentConfiguration;
import ocs.domain.ObservingProgram;
import ocs.ports.InstrumentGateway;
import ocs.ports.ObservingProgramRepository;

public class InstrumentSetupService {

    private final InstrumentGateway instrumentGateway;
    private final ObservingProgramRepository programRepo;

    public InstrumentSetupService(InstrumentGateway instrumentGateway, ObservingProgramRepository programRepo) {
        this.instrumentGateway = instrumentGateway;
        this.programRepo = programRepo;
    }

    public boolean setupInstrument(String programId) {
        ObservingProgram program = programRepo.findById(programId);
        if (program == null) {
            return false;
        }
        InstrumentConfiguration configuration = program.getConfiguration();
        String requiredConfig = configuration.getRequiredConfig();

        String actualConfig = instrumentGateway.readCurrentConfig();
        if (!configuration.matches(actualConfig)) {
            instrumentGateway.applyConfig(requiredConfig);
        }

        String verifiedConfig = instrumentGateway.readCurrentConfig();
        if (configuration.matches(verifiedConfig)) {
            configuration.markReady();
            programRepo.save(program);
            return true;
        }
        return false;
    }
}
