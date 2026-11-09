package ocs.cli;

import java.util.Scanner;

import ocs.application.InstrumentSetupService;
import ocs.application.ProgramExecutionService;
import ocs.application.ProgramSubmissionService;
import ocs.application.WarningConfirmationService;

public class OcsCli {

    private ProgramSubmissionService submissionService;
    private InstrumentSetupService setupService;
    private ProgramExecutionService executionService;
    private WarningConfirmationService warningService;

    public OcsCli(ProgramSubmissionService submissionService, InstrumentSetupService setupService,
            ProgramExecutionService executionService, WarningConfirmationService warningService) {
        this.submissionService = submissionService;
        this.setupService = setupService;
        this.executionService = executionService;
        this.warningService = warningService;
    }

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running && scanner.hasNextLine()) {
                running = execute(scanner.nextLine().trim().split("\\s+"));
            }
        }
    }

    public void submitProgram(String planId, String programId) {
        if (submissionService.submitProgram(planId, programId)) {
            System.out.println("Submitted");
        } else {
            System.out.println("Submission failed");
        }
    }

    public void setupInstrument(String programId) {
        if (setupService.setupInstrument(programId)) {
            System.out.println("Instrument ready");
        } else {
            System.out.println("Setup failed");
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
        } else {
            System.out.println("Commands: submit <planId> <programId> | setup <programId> | exit");
        }
        return true;
    }
}
