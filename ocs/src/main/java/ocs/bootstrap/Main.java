package ocs.bootstrap;

import java.io.PrintWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.time.Clock;
import java.util.Scanner;

import ocs.application.InstrumentSetupService;
import ocs.application.ProgramExecutionService;
import ocs.application.ProgramSubmissionService;
import ocs.application.ProgramValidator;
import ocs.application.WarningConfirmationService;
import ocs.cli.OcsCli;
import ocs.cli.ConsoleWarningDecisionPrompt;
import ocs.simulation.SimulatedInstrument;
import ocs.simulation.SimulatedTelescope;
import ocs.simulation.SimulatedWeather;
import ocs.storage.FileAuditRepository;
import ocs.storage.FileObservingProgramRepository;
import ocs.storage.FileSciencePlanRepository;

public class Main {

    public static void main(String[] args) {
        FileObservingProgramRepository programRepo = new FileObservingProgramRepository();
        Scanner scanner = new Scanner(System.in);
        PrintWriter output = new PrintWriter(new FileOutputStream(FileDescriptor.out), true);
        Clock clock = Clock.systemDefaultZone();
        WarningConfirmationService warningService =
                new WarningConfirmationService(programRepo, new FileAuditRepository(),
                        new ConsoleWarningDecisionPrompt(scanner, output, clock));
        OcsCli cli = new OcsCli(
                new ProgramSubmissionService(new FileSciencePlanRepository(), programRepo),
                new InstrumentSetupService(new SimulatedInstrument(), programRepo),
                new ProgramExecutionService(new ProgramValidator(programRepo), warningService,
                        new SimulatedTelescope(), new SimulatedWeather(), programRepo),
                scanner, output);
        cli.start();
    }
}
