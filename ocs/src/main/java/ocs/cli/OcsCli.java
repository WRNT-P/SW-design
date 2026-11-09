package ocs.cli;

import java.io.PrintWriter;
import java.util.Scanner;

import ocs.application.InstrumentSetupService;
import ocs.application.ProgramExecutionService;
import ocs.application.ProgramSubmissionService;

public class OcsCli {

    private final ProgramSubmissionService submissionService;
    private final InstrumentSetupService setupService;
    private final ProgramExecutionService executionService;
    private final Scanner scanner;
    private final PrintWriter output;

    public OcsCli(ProgramSubmissionService submissionService, InstrumentSetupService setupService,
            ProgramExecutionService executionService, Scanner scanner, PrintWriter output) {
        this.submissionService = submissionService;
        this.setupService = setupService;
        this.executionService = executionService;
        this.scanner = scanner;
        this.output = output;
    }

    public void start() {
        boolean running = true;
        while (running && scanner.hasNextLine()) {
            running = execute(scanner.nextLine().trim().split("\\s+"));
        }
    }

    public void submitProgram(String planId, String programId) {
        if (submissionService.submitProgram(planId, programId)) {
            output.println("Submitted");
        } else {
            output.println("Submission failed");
        }
    }

    public void setupInstrument(String programId) {
        if (setupService.setupInstrument(programId)) {
            output.println("Instrument ready");
        } else {
            output.println("Setup failed");
        }
    }

    public void startProgram(String programId) {
        if (executionService.startProgram(programId)) {
            output.println("Started: " + programId);
        } else {
            output.println("Start rejected: check submission, instrument, weather, telescope, "
                    + "and warning decisions");
        }
    }

    private boolean execute(String[] words) {
        String command = words[0];
        if ("exit".equals(command)) {
            return false;
        }
        if ("submit".equals(command) && words.length == 3) {
            submitProgram(words[1], words[2]);
        } else if ("setup".equals(command) && words.length == 2) {
            setupInstrument(words[1]);
        } else if ("start".equals(command) && words.length == 2) {
            startProgram(words[1]);
        } else {
            output.println("Commands: submit <planId> <programId> | setup <programId> | start <programId> | exit");
        }
        return true;
    }
}
