package ocs.application;

import ocs.ports.ObservingProgramRepository;
import ocs.ports.TelescopeGateway;
import ocs.ports.WeatherGateway;

public class ProgramExecutionService {

    private ProgramValidator validator;
    private WarningConfirmationService warningService;
    private TelescopeGateway telescopeGateway;
    private WeatherGateway weatherGateway;
    private ObservingProgramRepository programRepo;

    public ProgramExecutionService(ProgramValidator validator, WarningConfirmationService warningService,
            TelescopeGateway telescopeGateway, WeatherGateway weatherGateway, ObservingProgramRepository programRepo) {
        this.validator = validator;
        this.warningService = warningService;
        this.telescopeGateway = telescopeGateway;
        this.weatherGateway = weatherGateway;
        this.programRepo = programRepo;
    }

    public boolean startProgram(String programId) {
        throw new UnsupportedOperationException();
    }
}
