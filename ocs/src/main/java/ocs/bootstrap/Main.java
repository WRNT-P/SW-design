package ocs.bootstrap;

import ocs.application.InstrumentSetupService;
import ocs.application.ProgramExecutionService;
import ocs.application.ProgramSubmissionService;
import ocs.application.ProgramValidator;
import ocs.application.WarningConfirmationService;
import ocs.cli.OcsCli;
import ocs.simulation.SimulatedInstrument;
import ocs.simulation.SimulatedTelescope;
import ocs.simulation.SimulatedWeather;
import ocs.storage.FileAuditRepository;
import ocs.storage.FileObservingProgramRepository;
import ocs.storage.FileSciencePlanRepository;

public class Main {

    public static void main(String[] args) {
        FileObservingProgramRepository programRepo = new FileObservingProgramRepository();
        WarningConfirmationService warningService =
                new WarningConfirmationService(programRepo, new FileAuditRepository());
        OcsCli cli = new OcsCli(
                new ProgramSubmissionService(new FileSciencePlanRepository(), programRepo),
                new InstrumentSetupService(new SimulatedInstrument(), programRepo),
                new ProgramExecutionService(new ProgramValidator(programRepo), warningService,
                        new SimulatedTelescope(), new SimulatedWeather(), programRepo),
                warningService);
        cli.start();
    }
}
