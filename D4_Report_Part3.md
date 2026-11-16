# D4 Report — Part 3 (Traceability and Quality Evidence)

> Draft contribution for the team report. Merge these sections into `<groupname>_D4_Report.md` after replacing the clearly marked quality-check fields with evidence from the final commit. This draft is based on the repository snapshot at commit `58d3edc9532bd52df5fa5266dc9786ec6ab1367b` (2026-11-09).

## 4. Traceability Table

The tables link the use case steps to the corresponding messages in the sequence diagrams in `Chicken bomb_D4_Design.md` and to the current Java implementation under `ocs/src/main/java`.

### UC-04 — Submit Observing Program

| Use case step | Sequence diagram message | Class and method responsible |
|---|---|---|
| 1. Astronomer submits a program in a science plan | `Astronomer ->> Cli: submitProgram(planId, programId)` | `OcsCli.submitProgram(String, String)`; CLI input is dispatched by `OcsCli.execute(String[])` |
| 2. Retrieve the science plan | `SubSvc ->> PlanRepo: findById(planId)` | `ProgramSubmissionService.submitProgram(String, String)` calls `SciencePlanRepository.findById(String)`; implementation: `FileSciencePlanRepository.findById(String)` |
| 3. Find the program in the plan | `SubSvc ->> Plan: getProgram(programId)` | `ProgramSubmissionService.submitProgram(String, String)` calls `SciencePlan.getProgram(String)` |
| 4. Validate the program data | `SubSvc ->> Prog: validate()` | `ProgramSubmissionService.submitProgram(String, String)` calls `ObservingProgram.validate()` |
| 5. Mark submitted and persist | `SubSvc ->> Prog: submit()`; `SubSvc ->> ProgRepo: save(program)` | `ObservingProgram.submit()` then `ProgramSubmissionService.submitProgram(String, String)` calls `ObservingProgramRepository.save(ObservingProgram)`; implementation: `FileObservingProgramRepository.save(ObservingProgram)` |
| 6. Display the result | `Cli -->> Astronomer: Show result: Submitted` (or failure) | `OcsCli.submitProgram(String, String)` writes `Submitted` or `Submission failed` |

### UC-05 — Configure Planned Instrument Setup

| Use case step | Sequence diagram message | Class and method responsible |
|---|---|---|
| 1. Operator requests setup for a program | `TelescopeOperator ->> Cli: setupInstrument(programId)` | `OcsCli.setupInstrument(String)`; CLI input is dispatched by `OcsCli.execute(String[])` |
| 2. Retrieve the program and its required configuration | `SetupSvc ->> ProgRepo: findById(programId)`; `SetupSvc ->> Prog: getConfiguration()`; `SetupSvc ->> Config: getRequiredConfig()` | `InstrumentSetupService.setupInstrument(String)`; `ObservingProgram.getConfiguration()`; `InstrumentConfiguration.getRequiredConfig()` |
| 3. Read current instrument configuration and compare | `SetupSvc ->> Gateway: readCurrentConfig()`; `SetupSvc ->> Config: matches(actualConfig)` | `InstrumentSetupService.setupInstrument(String)`; `InstrumentGateway.readCurrentConfig()`; `InstrumentConfiguration.matches(String)` |
| 4. Apply required configuration when it differs | `SetupSvc ->> Gateway: applyConfig(requiredConfig)` | `InstrumentSetupService.setupInstrument(String)` calls `InstrumentGateway.applyConfig(String)`; prototype adapter: `SimulatedInstrument.applyConfig(String)` |
| 5. Read back and verify configuration | `SetupSvc ->> Gateway: readCurrentConfig()`; `SetupSvc ->> Config: matches(verifiedConfig)` | `InstrumentSetupService.setupInstrument(String)` calls `InstrumentGateway.readCurrentConfig()` and `InstrumentConfiguration.matches(String)` |
| 6. Mark ready, save, and report result | `SetupSvc ->> Config: markReady()`; `SetupSvc ->> ProgRepo: save(program)`; result to operator | `InstrumentConfiguration.markReady()`; `InstrumentSetupService.setupInstrument(String)`; `OcsCli.setupInstrument(String)` |

### UC-03 — Start Observing Program

| Use case step | Sequence diagram message | Class and method responsible |
|---|---|---|
| 1. Operator requests program start | `TO ->> cli: startProgram(programId)` | `OcsCli.startProgram(String)`; CLI input is dispatched by `OcsCli.execute(String[])` |
| 2. Validate existence, submitted status, required data, and warnings | `svc ->> val: validateProgram(programId, warningService)`; `val ->> repo: findById(programId)`; `val ->> prog: getStatus()` / `validate()` / `getWarnings()` | `ProgramExecutionService.startProgram(String)` calls `ProgramValidator.validateProgram(String, WarningConfirmationService)`; validator calls `ObservingProgramRepository.findById(String)`, `ObservingProgram.getStatus()`, `ObservingProgram.validate()`, and `ObservingProgram.getWarnings()` |
| 2b. Resolve each unresolved soft-error warning | `val ->> warnSvc: confirmWarning(...)`; prompt, warning resolution, audit, and save messages | `ProgramValidator.validateProgram(String, WarningConfirmationService)`; `WarningConfirmationService.confirmWarning(String, String)`; `WarningDecisionPrompt.requestDecision(String, SoftErrorWarning)`; `SoftErrorWarning.resolve(WarningDecision)`; `AuditRepository.logAction(String)`; repository `save(ObservingProgram)` |
| 3. Check instrument readiness | `svc ->> prog: getConfiguration()`; `svc ->> cfg: isInstrumentReady()` | `ProgramExecutionService.startProgram(String)` calls `ObservingProgram.getConfiguration()` and `InstrumentConfiguration.isInstrumentReady()` |
| 4. Check that the program is unlocked | `svc ->> prog: isLocked()` | `ProgramExecutionService.startProgram(String)` calls `ObservingProgram.isLocked()` |
| 5. Check weather | `svc ->> wx: checkSafeConditions()` | `ProgramExecutionService.startProgram(String)` calls `WeatherGateway.checkSafeConditions()`; prototype adapter: `SimulatedWeather.checkSafeConditions()` |
| 6. Start telescope observation | `svc ->> tel: startObservation()` | `ProgramExecutionService.startProgram(String)` calls `TelescopeGateway.startObservation()`; prototype adapter: `SimulatedTelescope.startObservation()` |
| 7. Start program/session and persist | `svc ->> prog: start()`; session creation; `svc ->> repo: save(program)` | `ProgramExecutionService.startProgram(String)` calls `ObservingProgram.start()` and then `ObservingProgramRepository.save(ObservingProgram)`; `ObservingProgram.start()` creates and starts an `ObservationSession` |
| 8. Display start result | `OcsCli` prints started or rejected result | `OcsCli.startProgram(String)` |

### UC-01 — Confirm Soft-Error Warning

UC-01 is reached as part of UC-03 validation; the interactive warning flow is initiated by the `start <programId>` CLI command.

| Use case step | Sequence diagram message | Class and method responsible |
|---|---|---|
| 1. Validation finds an unresolved warning | `VAL ->> WCS: confirmWarning(programId, warningId)` | `ProgramValidator.validateProgram(String, WarningConfirmationService)` calls `WarningConfirmationService.confirmWarning(String, String)` |
| 2. Retrieve program and warning, then present warning | `WCS ->> REPO: findById(programId)`; `WCS ->> PROG: getWarning(warningId)`; `WCS ->> PROMPT: requestDecision(...)`; prompt presents warning to observer | `WarningConfirmationService.confirmWarning(String, String)`; `ObservingProgram.getWarning(String)`; `WarningDecisionPrompt.requestDecision(String, SoftErrorWarning)`; console adapter: `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` |
| 3–4. Observer reviews and enters an ID and decision | Observer replies to the console prompt | `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` reads observer ID and proceed response from `Scanner` |
| 5. Create decision, resolve warning, audit and persist | Decision creation; `WCS ->> WRN: resolve(decision)`; `WCS ->> AUD: logAction(action)`; `WCS ->> REPO: save(program)` | `ConsoleWarningDecisionPrompt.decision(String, boolean)` creates `WarningDecision`; `WarningConfirmationService.confirmWarning(String, String)` calls `SoftErrorWarning.resolve(WarningDecision)`, `AuditRepository.logAction(String)`, and `ObservingProgramRepository.save(ObservingProgram)` |
| 6. Return to validation; reject if stop/no response | `WCS -->> VAL: true/false`; validation result returns to execution and CLI | `WarningConfirmationService.confirmWarning(String, String)` returns the decision outcome; `ProgramValidator.validateProgram(String, WarningConfirmationService)` and `ProgramExecutionService.startProgram(String)` propagate it; `OcsCli.startProgram(String)` displays the result |

## 5. SonarQube Results

**Scan result for the current workspace:** SonarQube for IDE automatic analysis was enabled in VS Code. The SonarQube Findings panel displayed “No SonarQube issues to display.” The scan was observed on 2026-11-17 with the installed SonarQube for IDE extension version 6.3.0. The workspace `HEAD` was `58d3edc9532bd52df5fa5266dc9786ec6ab1367b`; this is the commit available for this scan and should only be described as the final commit if the team confirms it is the submitted final commit. The result below reflects the currently analyzed code and is not a claim about any later changes.

| Scan date | Commit scanned | SonarQube for IDE version | Issues (rule, file:line) | Fix or reason for keeping each issue |
|---|---|---|---|---|
| 2026-11-17 | `58d3edc9532bd52df5fa5266dc9786ec6ab1367b` | SonarQube for IDE 6.3.0 | No issues reported by the Findings panel | No fixes required for the issues reported in this scan. Re-scan if source changes before the final commit. |

## 6. AI Review

The review below is a preliminary review of the available repository commit, not the team's final commit. The project brief requires the team to run Appendix A against its final commit and retain the complete result in `reviews/`; rerun and update this section after the final commit if it differs.

| Review date | AI tool and exact model/version | Commit reviewed | File access method | Findings and team's response |
|---|---|---|---|---|
| 2026-11-17 | OpenAI Codex, GPT-6 (exact build/version not exposed in this session) | `58d3edc9532bd52df5fa5266dc9786ec6ab1367b` | Read the checked-out repository files in the local workspace | Finding 1: UC-05's alternative mentions a not-submitted program even though the use case precondition requires submission; code relies on the current repository lifecycle to make unsubmitted programs unavailable rather than explicitly checking status. Team decision: `TODO: reconcile the precondition/alternative and decide whether to add an explicit status guard and sequence message.` |

Full review output: [reviews/AI_Review_58d3edc.md](reviews/AI_Review_58d3edc.md).

## 7. AI Usage Declaration

This table records the AI use for preparing this contribution, including the preliminary review above. Add the other team members' AI use when the sections are combined. The model identifier available in this session is GPT-6; no more specific build/version identifier was exposed to the author, so the team should verify the exact model label in the client before submission if available.

| AI tool and exact model | Purpose | Files or parts produced or changed | How the team checked the output |
|---|---|---|---|
| OpenAI Codex, GPT-6 (exact build/version not exposed in this session) | Drafted traceability mappings by comparing the team's design diagrams with Java classes/methods; performed a preliminary design/code review of the checked-out commit | Created `D4_Report_Part3.md` and `reviews/AI_Review_58d3edc.md`; no source code or design file changed | Compared mappings and review finding against `Chicken bomb_D4_Design.md` and the current Java source; team must verify the finding, decide its disposition, rerun review on the final commit, and fill SonarQube evidence from the actual final-commit scan |
