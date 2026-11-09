package ocs.cli;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;

import ocs.domain.SoftErrorWarning;
import ocs.domain.WarningDecision;
import ocs.ports.WarningDecisionPrompt;

public class ConsoleWarningDecisionPrompt implements WarningDecisionPrompt {

    private final Scanner scanner;
    private final PrintWriter output;
    private final Clock clock;

    public ConsoleWarningDecisionPrompt(Scanner scanner, PrintWriter output, Clock clock) {
        this.scanner = scanner;
        this.output = output;
        this.clock = clock;
    }

    @Override
    public WarningDecision requestDecision(String programId, SoftErrorWarning warning) {
        output.println("Soft error for " + programId + " [" + warning.getWarningId() + "]: "
                + warning.getCondition());
        output.print("Observer ID: ");
        output.flush();
        if (!scanner.hasNextLine()) {
            return decision("NO_RESPONSE", false);
        }
        String observerId = scanner.nextLine().trim();
        if (observerId.isEmpty() || !scanner.hasNextLine()) {
            return decision(observerId.isEmpty() ? "NO_RESPONSE" : observerId, false);
        }
        output.print("Proceed with observation? (y/n): ");
        output.flush();
        boolean proceed = "y".equalsIgnoreCase(scanner.nextLine().trim());
        return decision(observerId, proceed);
    }

    private WarningDecision decision(String observerId, boolean proceed) {
        return new WarningDecision(UUID.randomUUID().toString(), proceed ? "PROCEED" : "STOP",
                observerId, LocalDateTime.now(clock));
    }
}
