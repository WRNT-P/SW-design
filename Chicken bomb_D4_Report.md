# Chicken bomb D4 Report

This report describes the design in `Chicken bomb_D4_Design.md` and the Java code in `ocs/`. The implementation was originally aligned at commit `58d3edc`; fixes from AI Review run 1 are reflected in the current working tree and listed in Section 6 with a commit placeholder for the team to fill after committing.

## 1. Design Rationale

The design splits the OCS into seven packages with one-way dependencies, gives each use case its own application service, reaches every external system, the data store and the Science Observer's decision through an interface in `ocs.ports`, and creates all concrete classes in `ocs.bootstrap.Main`.

### 1.1 Cohesion

| Package | Responsibility |
|---|---|
| `ocs.bootstrap` | Creates and wires all objects (`Main`) |
| `ocs.cli` | Reads commands and prints results (`OcsCli`), asks the Science Observer for a decision (`ConsoleWarningDecisionPrompt`) |
| `ocs.application` | One service per use case, plus `ProgramValidator` for Validate Program |
| `ocs.domain` | Entities and their rules |
| `ocs.ports` | Interfaces for the data store, external systems and the decision prompt |
| `ocs.storage` | File-based repositories |
| `ocs.simulation` | Simulated instrument, telescope and weather system |

Each application service has one public operation for one use case: `ProgramSubmissionService.submitProgram` (UC-04), `InstrumentSetupService.setupInstrument` (UC-05), `ProgramExecutionService.startProgram` (UC-03) and `WarningConfirmationService.confirmWarning` (UC-01). `ProgramValidator.validateProgram` holds the Validate Program steps that UC-03 includes and UC-01 extends. Rules about an entity's data are kept in that entity: `ObservingProgram.submit()` and `start()`, `InstrumentConfiguration.matches()` and `markReady()`, `SoftErrorWarning.resolve()` and `WarningDecision.isProceed()`. `OcsCli` contains no business rules. It turns a service's `boolean` result into a message.

One method has weaker cohesion. `OcsCli.execute(String[])` selects one of four actions from the command word, which is logical cohesion. We kept it because it is private, it only parses input and calls one public `OcsCli` operation per command, and it is limited to the command-line boundary.

### 1.2 Coupling

The imports in the code follow the package diagram, and the dependencies form no cycle. `ocs.application` imports only `ocs.ports` and `ocs.domain`. `ocs.storage` and `ocs.simulation` import `ocs.ports`. `ocs.storage` also imports `ocs.domain`. `ocs.simulation` does not import `ocs.domain`, although the diagram allows it. Only `Main` imports the concrete classes in `ocs.storage`, `ocs.simulation` and `ocs.cli`.

Most interactions are data coupling. Services take identifiers (`String`) and return `boolean`. The gateways exchange `String` and `boolean` values. Where a whole domain object is passed, as in `ObservingProgramRepository.save(ObservingProgram)` and `WarningDecisionPrompt.requestDecision(String, SoftErrorWarning)`, the coupling is stamp coupling. `ConsoleWarningDecisionPrompt` only uses `getWarningId()` and `getCondition()` of the warning.

Two coupling choices are worth noting:

- **Constructor-injected service association.** `ProgramValidator` receives `WarningConfirmationService` through its constructor and keeps it for warning confirmation. `ProgramExecutionService` depends only on `ProgramValidator` and no longer carries or passes the warning service on each call.
- **Reaching through objects.** `ProgramValidator` calls `warning.getDecision().isProceed()`. `ProgramExecutionService` and `InstrumentSetupService` call `program.getConfiguration()` and then call `InstrumentConfiguration` directly. These services therefore depend on how the `ObservingProgram` aggregate is structured. We accepted this because each of these calls is shown in a sequence diagram and stays within `ocs.domain` types.

### 1.3 Connascence

Across packages, most connascence is of name and type. Services depend on the operation names and parameter types of the port interfaces, and the compiler checks both. The constructor with five parameters (`OcsCli`) and the constructor with four parameters (`ProgramExecutionService`) use different types at each position, so a wrong order fails to compile.

Stronger forms remain in the following places:

| Form | Where | Why it was kept or how it is limited |
|---|---|---|
| Meaning (literal) | `"Submitted"` is set in `ObservingProgram.submit()` and compared in `ProgramValidator.validateProgram()` | The class diagram types `status` as `String`. An `ObservingProgram.isSubmitted()` operation would move the comparison into the class that sets the value. |
| Meaning (literal) | `"PROCEED"`/`"STOP"` is created in `ConsoleWarningDecisionPrompt.decision()`, read in `WarningDecision.isProceed()`, and created again from `isProceed()` for the audit text in `WarningConfirmationService.confirmWarning()` | `isProceed()` hides the literal from `ocs.application`, but the prompt in `ocs.cli` must still use the same spelling. An enum in `ocs.domain` would replace this with connascence of name. |
| Meaning (literal) | `"Running"` is set in both `ObservingProgram.start()` and `ObservationSession.start()` | No code reads it yet, so it is a shared convention only. |
| Convention | `null` means "not found" for `findById`, `SciencePlan.getProgram` and `ObservingProgram.getWarning`, and every caller checks for `null` | The sequence diagrams model the alternatives as `= null` checks. |
| Position | `WarningDecision(decisionId, outcome, decidedBy, decidedAt)` takes three `String` parameters; `submitProgram(planId, programId)` and `confirmWarning(programId, warningId)` take two `String` parameters each | Swapping these parameters would still compile. The `WarningDecision` constructor is called in only one place, `ConsoleWarningDecisionPrompt.decision()`. |
| Execution order | `ProgramExecutionService.startProgram` must call `programRepo.findById` again after `validateProgram` returns, because `confirmWarning` saved its own copy of the program with the decision | All three classes are in `ocs.application`, and the order matches the UC-03 sequence diagram. |

### 1.4 Data Store

We store data in local files using only the JDK. `FileSciencePlanRepository` and `FileObservingProgramRepository` write a `Map` of objects to `data/science-plans.ser` and `data/programs.ser` with Java object serialization (`ObjectOutputStream`/`ObjectInputStream`). `FileAuditRepository` appends lines to the text file `data/audit.log` with `java.nio.file.Files`.

| Constraint | How the choice meets it |
|---|---|
| Java 17 and Maven | `pom.xml` sets `maven.compiler.release` to 17 and uses only JDK APIs |
| At most one external library, only for the data store | `pom.xml` declares no dependencies, so the one allowed library is not used |
| No separate server | The data store consists of files in the working directory |
| Creates its files on first run | Each repository constructor creates `data/` and its file if they are missing. `FileSciencePlanRepository` also stores the sample plan `SP-01` |
| Runs from a fresh clone | `data/` is listed in `.gitignore`, so a fresh clone starts without data and the files are created on first run |

We did not choose an embedded database such as H2. It would add a dependency, a schema and code that maps SQL rows to six domain classes, while the prototype stores only a few objects. Our choice has the following costs:

- Every `save` reads and rewrites the whole file.
- The domain classes declare no `serialVersionUID`. After a domain class changes, existing `.ser` files may no longer load, and `data/` must be deleted.
- All six domain classes implement `java.io.Serializable`, as the class diagram shows. This is the only place where the choice of data store reaches `ocs.domain`.
- Plans and programs are stored in separate files. The copy of a program inside `science-plans.ser` is never updated. Running `submit` again for a program that has already started therefore replaces its record in `programs.ser` with that original copy, which is unlocked, not ready and has no warning decisions. This is a known limitation of the current code.

The rest of the code does not depend on this choice. `ocs.application` uses only `SciencePlanRepository`, `ObservingProgramRepository` and `AuditRepository`, and imports nothing from `java.io` or `ocs.storage`. Only `Main.main` names the `File…Repository` classes. To move to H2, we would add new adapters to `ocs.storage` and change the three constructor calls in `Main`. No service or CLI class would change.

### 1.5 Design Pattern: Ports and Adapters

**Problem.** The four use cases need three external systems: the instrument (UC-05) and the weather and telescope systems (UC-03). All four use cases need the data store. UC-01 also needs a decision from the Science Observer in the middle of UC-03's validation. The project requires an interface and a simulated version for each external system. The use case logic should not change when a simulation is replaced by real hardware or the files are replaced by a database.

**Roles.** Every class below appears in both the class diagram, with the role named in its note, and the code.

| Role | Classes |
|---|---|
| Port (interface used by the application) | `SciencePlanRepository`, `ObservingProgramRepository`, `AuditRepository`, `InstrumentGateway`, `TelescopeGateway`, `WeatherGateway`, `WarningDecisionPrompt` (`ocs.ports`) |
| Adapter (implements a port) | `FileSciencePlanRepository`, `FileObservingProgramRepository`, `FileAuditRepository` (`ocs.storage`); `SimulatedInstrument`, `SimulatedTelescope`, `SimulatedWeather` (`ocs.simulation`); `ConsoleWarningDecisionPrompt` (`ocs.cli`) |
| Application core (uses ports only) | `ProgramSubmissionService`, `InstrumentSetupService`, `ProgramExecutionService`, `ProgramValidator`, `WarningConfirmationService` |
| Composition root (constructor-based dependency injection) | `Main.main(String[])` |

UC-01 shows the pattern most clearly. `WarningConfirmationService.confirmWarning` needs a `WarningDecision`, so it calls `decisionPrompt.requestDecision(programId, warning)` without knowing that a console exists. `ConsoleWarningDecisionPrompt` adapts `Scanner`, `PrintWriter` and `Clock`, which are all shown in the class diagram, to that port. It returns a `WarningDecision`, and when no answer is given it returns a STOP decision with `"NO_RESPONSE"` as the decision maker.

**Why not a simpler alternative.** The simplest alternative is for each service to create its collaborators directly, for example `new SimulatedTelescope()` inside `ProgramExecutionService`, and for `WarningConfirmationService` to read `System.in` itself. This needs fewer types. However, `ocs.application` would then depend on `ocs.simulation`, `ocs.storage` and the console, which the package diagram does not allow, and replacing any of them would mean editing the services. A second alternative is for `OcsCli` to ask about the warnings before it calls `startProgram`. That would move the rules about unresolved and previously declined warnings out of `ProgramValidator` into the CLI, and it would no longer match UC-01, where the question arises inside Validate Program. The pattern has costs too: there are seven interfaces with one implementation each, and `Main` must know every concrete class. The design would also let tests replace the adapters with test doubles, but the prototype has no automated tests (`ocs/src/test` does not exist), so that benefit is not yet used.

### 1.6 Use Case Owners

| Use case | Owner |
|---|---|
| UC-04 Submit Observing Program | Madhuri Singh |
| UC-05 Configure Planned Instrument Setup | Nattanan Jumparuang |
| UC-03 Start Observing Program | Phoonyaphat Junsiri and Barameeporn Niyomtrong |
| UC-01 Confirm Soft-Error Warning | Woraneti |

These owners are the Phase 1 authors of each use case. The team must confirm them before submission.

## 2. How to Run

### Requirements and setup

- Install a JDK that supports Java 17 and Apache Maven.
- Open a terminal and change to the `ocs/` directory in the project.
- No external service or database configuration is required. On first launch, the application creates `data/science-plans.ser` with sample science plan `SP-01`, programs `OP-01` and `OP-02`, and a `WARN-01` soft-error warning on `OP-01`. It also creates `data/programs.ser` for submitted programs and `data/audit.log` for warning decisions. These files are relative to the current working directory, so run the application from `ocs/`.
- The CLI has no login step. The authentication preconditions in the use case descriptions are outside this prototype.

### Compile and launch

```sh
cd ocs
mvn clean package
java -jar target/ocs-1.0.jar
```

The CLI reads one command per line. Enter `exit` to quit. Supported commands:

```text
submit <planId> <programId>
setup <programId>
start <programId>
exit
```

### Typical end-to-end use

Run the commands in this order (the warning prompt appears during `start`):

```text
submit SP-01 OP-01
setup OP-01
start OP-01
OBSERVER-01
y
exit
```

Expected output (the prompts are printed without a newline, so the next output follows on the same line):

```text
Submitted
Instrument ready
Soft error for OP-01 [WARN-01]: High airmass may reduce image quality
Observer ID: Proceed with observation? (y/n): Started: OP-01
```

The warning decision is written to `data/audit.log`. Program state (status, instrument readiness, lock and warning decisions) persists in `data/programs.ser` between runs. A warning resolved with `PROCEED` is not asked again, and a program whose warning was resolved with `STOP` is rejected on every later start. Running `submit` again for a program copies the original program from the science plan and clears this state. To restore the initial sample state, delete the `data/` directory before starting the application.

## 3. Use Case Execution

The examples below use the sample records created on first launch. Each example starts from a fresh state, so delete `ocs/data/` before running it. `OP-01` is valid and has required configuration `GMOS:R400:r`. `OP-02` has no configuration and therefore fails submission validation. Inputs are entered one per line.

### UC-04 — Submit Observing Program

**Main flow:** The astronomer submits a program belonging to a science plan. The application loads the plan, finds the program, validates its required configuration, marks it `Submitted`, persists it, and reports success.

**Test input:**

```text
submit SP-01 OP-01
```

**Expected result:** `Submitted`; `OP-01` is saved with status `Submitted`.

**Alternative flows:**

| Case | Input | Expected result |
|---|---|---|
| Science plan does not exist (2a) | `submit SP-404 OP-01` | `Submission failed` |
| Program is not in the selected plan (3a) | `submit SP-01 OP-404` | `Submission failed` |
| Program has incomplete or invalid required information (4a) | `submit SP-01 OP-02` | `Submission failed`; `OP-02` is not submitted because its configuration is missing |

### UC-05 — Configure Planned Instrument Setup

**Main flow:** The operator requests setup. The application retrieves the program and required configuration, compares it with the instrument's current configuration, applies it when different, reads it back, marks the configuration ready, saves the program, and reports success. The simulated instrument starts without a configuration, so the first successful setup applies `GMOS:R400:r`.

**Test input:**

```text
submit SP-01 OP-01
setup OP-01
```

**Expected result:** `Submitted`, then `Instrument ready`; the configuration is marked ready and saved.

**Alternative flows:**

| Case | Input | Expected result |
|---|---|---|
| Program not found (2a) | `setup OP-404` | `Setup failed` |
| Program not submitted (2a) | `setup OP-01` on a fresh state, before any `submit` | `Setup failed` |
| Program already started (2a) | `submit SP-01 OP-01`, `setup OP-01`, `start OP-01`, `OBSERVER-01`, `y`, `setup OP-01` | The program starts, then the final setup attempt reports `Setup failed` because its status is `Running` |
| Current configuration already matches (3a) | `submit SP-01 OP-01`, `setup OP-01`, `setup OP-01` in the same running process | The second setup skips applying a new configuration, verifies the current configuration, and reports `Instrument ready` |
| Verification unsuccessful (5a) | Not reproducible through the current CLI: `SimulatedInstrument.applyConfig` always succeeds and stores the requested configuration | The failure branch exists in `InstrumentSetupService`, but this prototype offers no CLI input or adapter setting to force a verification mismatch |

A program that has not been submitted is not stored in `data/programs.ser`, so the fresh-state CLI example rejects it as not found (`setup OP-01` before submission and `setup OP-02` both print `Setup failed`). `InstrumentSetupService` also checks the stored program status and rejects any status other than `Submitted`, including a program that has already started.

### UC-03 — Start Observing Program

**Main flow:** The operator starts a submitted program. The application validates its status and required information, resolves warnings, checks instrument readiness, lock status, and weather, asks the telescope to start, then marks the program `Running`, locks it, creates and starts an observation session, persists it, and reports success.

**Test input:**

```text
submit SP-01 OP-01
setup OP-01
start OP-01
OBSERVER-01
y
```

**Expected result:** After the warning decision is recorded, `Started: OP-01`. In the current simulation, weather is always safe and the telescope always starts successfully.

**Alternative flows:**

| Case | Input | Expected result |
|---|---|---|
| Missing, invalid, or not-submitted program (2a) | `start OP-404`, or `start OP-01` on a fresh state before `submit` | `Start rejected: check submission, instrument, weather, telescope, and warning decisions` |
| Observer declines a warning (2b) | `submit SP-01 OP-01`, `setup OP-01`, `start OP-01`, `OBSERVER-01`, `n` | A `STOP` decision is recorded; `Start rejected: …` |
| Instrument not ready (3a) | `submit SP-01 OP-01`, `start OP-01`, `OBSERVER-01`, `y` | `Start rejected: …` because setup was not run. Then `setup OP-01` and `start OP-01` give `Instrument ready` and `Started: OP-01` without a second warning prompt, because the warning is already resolved with `PROCEED` |
| Program locked (4a) | Not reproducible through the current CLI: a started program has status `Running` and is rejected at validation (2a) before the lock check, and no use case calls `ObservingProgram.lock()` | The rejection branch exists in `ProgramExecutionService` |
| Unsafe weather (5a) | Not reproducible through the current CLI: `SimulatedWeather.checkSafeConditions()` always returns `true` | The rejection branch exists in `ProgramExecutionService`, but no CLI input or adapter setting can simulate unsafe weather |
| Telescope fails to start (6a) | Not reproducible through the current CLI: `SimulatedTelescope.startObservation()` always returns `true` | The rejection branch exists in `ProgramExecutionService`, but no CLI input or adapter setting can simulate telescope failure |

### UC-01 — Confirm Soft-Error Warning

UC-01 is invoked within UC-03 validation by `start <programId>` when an unresolved warning is found; it is not a separate CLI command.

**Main flow:** Start a valid, submitted, instrument-ready `OP-01`. At the warning prompt, enter an observer ID and `y`. The application creates a timestamped `PROCEED` decision, resolves `WARN-01`, records the audit action, saves the program, returns to validation, and continues UC-03.

**Test input:**

```text
submit SP-01 OP-01
setup OP-01
start OP-01
OBSERVER-01
y
exit
```

**Expected result:** The warning is shown as `Soft error for OP-01 [WARN-01]: High airmass may reduce image quality`, followed by `Started: OP-01`. `data/audit.log` contains a line with `decision=PROCEED by=OBSERVER-01`.

**Alternative flows:**

| Case | Input | Expected result |
|---|---|---|
| More than one unresolved warning (2a) | No sample with multiple warnings is provided | The code loops through the warnings one by one; each needs an observer ID and decision before validation completes |
| Observer does not proceed (4a) | `submit SP-01 OP-01`, `setup OP-01`, `start OP-01`, `OBSERVER-01`, `n` | A timestamped `STOP` decision by `OBSERVER-01` is recorded; `Start rejected: …` |
| No observer ID or response (4b) | `submit SP-01 OP-01`, `setup OP-01`, `start OP-01`, then an empty line; alternatively, enter an observer ID and end input before the proceed answer | A `STOP` decision with `NO_RESPONSE` as the decision maker is recorded; `Start rejected: …` |

The prompt has no timeout mechanism. A blank observer ID and the end of input are the implemented no-response cases.

## 4. Traceability Table

The tables link each use case step to the messages of its sequence diagram in `Chicken bomb_D4_Design.md`, written as in the Mermaid source, and to the class and method in `ocs/src/main/java` that sends each message. Where an actor sends a message, the table names the actor and the method that reads its input.

### UC-04 — Submit Observing Program

| Use case step | Sequence diagram message | Class and method that sends the message |
|---|---|---|
| 1. Astronomer selects a plan and a program and chooses Submit | `Astronomer->>Cli: submitProgram(planId, programId)` | Astronomer, through the command `submit <planId> <programId>`, read and dispatched by `OcsCli.execute(String[])` |
| 1. (continued) | `Cli->>SubSvc: submitProgram(planId, programId)` | `OcsCli.submitProgram(String, String)` |
| 2. Retrieve the science plan | `SubSvc->>PlanRepo: findById(planId)` | `ProgramSubmissionService.submitProgram(String, String)` |
| 3. Find the program in the plan | `SubSvc->>Plan: getProgram(programId)` | `ProgramSubmissionService.submitProgram(String, String)` |
| 4. Check the program data | `SubSvc->>Prog: validate()` | `ProgramSubmissionService.submitProgram(String, String)` |
| 5. Set status Submitted and store the program | `SubSvc->>Prog: submit()`<br>`SubSvc->>ProgRepo: save(program)` | `ProgramSubmissionService.submitProgram(String, String)` |
| 6. Display the result | `SubSvc-->>Cli: true` / `false` | `ProgramSubmissionService.submitProgram(String, String)` (return) |
| 6. (continued) | `Cli-->>Astronomer: Show result: Submitted` (or `Submission failed`) | `OcsCli.submitProgram(String, String)` |

### UC-05 — Configure Planned Instrument Setup

| Use case step | Sequence diagram message | Class and method that sends the message |
|---|---|---|
| 1. Operator selects the program and starts setup | `TelescopeOperator->>Cli: setupInstrument(programId)` | Telescope Operator, through the command `setup <programId>`, read and dispatched by `OcsCli.execute(String[])` |
| 1. (continued) | `Cli->>SetupSvc: setupInstrument(programId)` | `OcsCli.setupInstrument(String)` |
| 2. Retrieve the program, require Submitted status, and get its required configuration | `SetupSvc->>ProgRepo: findById(programId)`<br>`SetupSvc->>Prog: getStatus()`<br>`SetupSvc->>Prog: getConfiguration()`<br>`SetupSvc->>Config: getRequiredConfig()` | `InstrumentSetupService.setupInstrument(String)` |
| 2a. Program not found or not submitted | `SetupSvc-->>Cli: false` | `InstrumentSetupService.setupInstrument(String)` |
| 3. Read the current configuration and compare | `SetupSvc->>Gateway: readCurrentConfig()`<br>`SetupSvc->>Config: matches(actualConfig)` | `InstrumentSetupService.setupInstrument(String)` |
| 4. Send the required configuration to the ICS | `SetupSvc->>Gateway: applyConfig(requiredConfig)` | `InstrumentSetupService.setupInstrument(String)` |
| 5. Read the configuration again and verify | `SetupSvc->>Gateway: readCurrentConfig()`<br>`SetupSvc->>Config: matches(verifiedConfig)` | `InstrumentSetupService.setupInstrument(String)` |
| 6. Mark ready and record with the program | `SetupSvc->>Config: markReady()`<br>`SetupSvc->>ProgRepo: save(program)` | `InstrumentSetupService.setupInstrument(String)` |
| 6. Display the result | `SetupSvc-->>Cli: true` / `false` | `InstrumentSetupService.setupInstrument(String)` (return) |
| 6. (continued) | `Cli-->>TelescopeOperator: Show result: Instrument ready` (or `Setup failed`) | `OcsCli.setupInstrument(String)` |

### UC-03 — Start Observing Program

| Use case step | Sequence diagram message | Class and method that sends the message |
|---|---|---|
| 1. Operator commands the system to start the program | `TO->>cli: startProgram(programId)` | Telescope Operator, through the command `start <programId>`, read and dispatched by `OcsCli.execute(String[])` |
| 1. (continued) | `cli->>svc: startProgram(programId)` | `OcsCli.startProgram(String)` |
| 2. Validate the program | `svc->>val: validateProgram(programId)` | `ProgramExecutionService.startProgram(String)` |
| 2. (continued) | `val->>repo: findById(programId)`<br>if found: `val->>prog: getStatus()` then `val->>prog: validate()`<br>if valid: `val->>prog: getWarnings()` | `ProgramValidator.validateProgram(String)` |
| 2a. Program not found | `val-->>svc: false` | `ProgramValidator.validateProgram(String)` |
| 2a. Program not submitted or invalid | `val-->>svc: false` after `getStatus()` and `validate()` | `ProgramValidator.validateProgram(String)` |
| 2b. Handle each soft-error warning | `val->>warning: isResolved()`<br>`val->>warnSvc: confirmWarning(programId, warningId)`<br>`val->>warning: getDecision()`<br>`val->>dec: isProceed()` | `ProgramValidator.validateProgram(String)` |
| 2b. (UC-01 inside UC-03) | `warnSvc->>repo: findById(programId)`<br>`warnSvc->>prog: getWarning(warningId)`<br>`warnSvc->>prompt: requestDecision(programId, warning)`<br>`warnSvc->>warning: resolve(decision)`<br>`warnSvc->>audit: logAction(action)`<br>`warnSvc->>repo: save(program)` | `WarningConfirmationService.confirmWarning(String, String)` |
| 2b. (UC-01 inside UC-03) | `prompt-->>SO: Present warning (program + condition)` | `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` |
| 2b. (UC-01 inside UC-03) | `SO-->>prompt: proceed or stop, observerId` or missing input; `prompt->>dec: «create» WarningDecision(...)`, including `"STOP", "NO_RESPONSE"` for missing input | Science Observer; input and decision creation handled by `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` and `decision(String, boolean)` |
| 3. Check that the instrument is ready | `svc->>repo: findById(programId)`<br>`svc->>prog: getConfiguration()`<br>`svc->>cfg: isInstrumentReady()` | `ProgramExecutionService.startProgram(String)` |
| 4. Check that the program is not locked | `svc->>prog: isLocked()` | `ProgramExecutionService.startProgram(String)` |
| 5. Check the weather | `svc->>wx: checkSafeConditions()` | `ProgramExecutionService.startProgram(String)` |
| 6. Command the telescope to start | `svc->>tel: startObservation()` | `ProgramExecutionService.startProgram(String)` |
| 7. Mark started, start a session and store the program | `svc->>prog: start()`<br>`svc->>repo: save(program)` | `ProgramExecutionService.startProgram(String)` |
| 7. (continued) | `prog->>sess: «create» ObservationSession(sessionId)`<br>`prog->>sess: start()` | `ObservingProgram.start()` |
| 8. Display the start result | `svc-->>cli: true` / `false` | `ProgramExecutionService.startProgram(String)` (return); the result is printed by `OcsCli.startProgram(String)` (Note in the diagram) |

### UC-01 — Confirm Soft-Error Warning

UC-01 is reached during UC-03 validation; the diagram starts with the `start <programId>` command.

| Use case step | Sequence diagram message | Class and method that sends the message |
|---|---|---|
| (UC-03 start) | `TO->>CLI: startProgram(programId)` | Telescope Operator, through `start <programId>`, read and dispatched by `OcsCli.execute(String[])` |
| (UC-03 start) | `CLI->>EXE: startProgram(programId)` | `OcsCli.startProgram(String)` |
| (UC-03 start) | `EXE->>VAL: validateProgram(programId)` | `ProgramExecutionService.startProgram(String)` |
| 1. Validate Program finds an unresolved soft-error warning | `VAL->>REPO: findById(programId)`<br>`VAL->>PROG: getWarnings()`<br>`VAL->>WCS: confirmWarning(programId, warningId)` | `ProgramValidator.validateProgram(String)` |
| 2. Present the warning to the Science Observer | `WCS->>REPO: findById(programId)`<br>`WCS->>PROG: getWarning(warningId)`<br>`WCS->>PROMPT: requestDecision(programId, warning)` | `WarningConfirmationService.confirmWarning(String, String)` |
| 2. (continued) | `PROMPT-->>SO: Present warning (program + condition)` | `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` |
| 3–4. Observer reviews and enters an ID and decision | `SO-->>PROMPT: proceed or stop, observerId`, or missing input | Science Observer; input read by `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` |
| 5. Create the decision | `PROMPT->>DEC: «create» WarningDecision(decisionId, outcome, decidedBy, decidedAt)`; no ID/no answer uses `"STOP", "NO_RESPONSE"` | `ConsoleWarningDecisionPrompt.decision(String, boolean)`, called from `requestDecision` |
| 5. (continued) | `PROMPT-->>WCS: decision` | `ConsoleWarningDecisionPrompt.requestDecision(String, SoftErrorWarning)` (return) |
| 5. Resolve, audit and store | `WCS->>WRN: resolve(decision)`<br>`WCS->>AUD: logAction(action)`<br>`WCS->>REPO: save(program)` | `WarningConfirmationService.confirmWarning(String, String)` |
| 6. Return control to Validate Program | `WCS-->>VAL: true` | `WarningConfirmationService.confirmWarning(String, String)` (return) |
| 4a/4b. Observer does not proceed | `WCS-->>VAL: false`<br>`VAL-->>EXE: false`<br>`EXE-->>CLI: false` | Returns of `confirmWarning`, `ProgramValidator.validateProgram` and `ProgramExecutionService.startProgram` |
| 4a/4b. (continued) | `CLI-->>TO: Start rejected` | `OcsCli.startProgram(String)` |

## 5. SonarQube Results

SonarQube for IDE automatic analysis was enabled in VS Code. The SonarQube Findings panel displayed "No SonarQube issues to display."

| Scan date | Commit scanned | SonarQube for IDE version | Issues (rule, file:line) | Fix or reason for keeping each issue |
|---|---|---|---|---|
| 2026-11-17 | `58d3edc9532bd52df5fa5266dc9786ec6ab1367b` | SonarQube for IDE 6.3.0 | No issues reported by the Findings panel | No fixes required |

## 6. AI Review

| Review date | AI tool and exact model/version | Commit reviewed | File access method |
|---|---|---|---|
| 2026-11-17 | OpenAI Codex, GPT-6 (exact build identifier not exposed) | `410aa9356912073a006b44d1b7eb9e8403800230` | Read the combined report, design and checked-out Java source files in the local repository |

| No. | Finding | Team's response |
|---|---|---|
| 1 | UC-05 precondition and alternative 2a were inconsistent about unsubmitted programs. | `Fixed in commit <hash>` |
| 2 | UC-01 sequence omitted the no-ID/no-answer branch that creates a STOP decision with `NO_RESPONSE`. | `Fixed in commit <hash>` |
| 3 | `OcsCli` fields were `final` in code but were not marked `readOnly` in the class diagram. | `Fixed in commit <hash>` |
| 4 | `InstrumentSetupService` allowed setup when a stored program was not Submitted. | `Fixed in commit <hash>` |
| 5 | `ProgramValidator` and the UC-03 sequence did not agree on null, status and validation branches. | `Fixed in commit <hash>` |
| 6 | The package diagram shows project packages but omits Java standard-library dependencies used by the source. | Pending team decision: show the JDK package dependencies or state that the package diagram covers project packages only. |
| 7 | `OcsCli.execute` has logical cohesion. | Kept as a private command-line dispatcher, as explained in Section 1.1. |
| 8 | `ProgramExecutionService` holds five collaborator references. | Noted; consider extracting a focused collaborator if the service grows. |
| 9 | Repository `save` calls pass the full `ObservingProgram` aggregate (stamp coupling). | Accepted as the repository persistence contract, as explained in Section 1.2. |
| 10 | `WarningDecisionPrompt` receives a full `SoftErrorWarning` although the console adapter uses only the ID and condition (stamp coupling). | Accepted for this prototype; a narrow warning view is an option if the boundary needs to change. |
| 11 | `ProgramExecutionService` passed `WarningConfirmationService` to `ProgramValidator` on each validation call. | `Fixed in commit <hash>` |
| 12 | `ProgramValidator` and the setup/execution services reach through domain objects to access nested decision/configuration data. | Accepted and documented in Section 1.2; intent-level domain operations may reduce this coupling. |

Full review output: [reviews/AI_Review_410aa93.md](reviews/AI_Review_410aa93.md).

## 7. AI Usage Declaration

| AI tool and exact model | Purpose | Files or parts produced or changed | How the team checked the output |
|---|---|---|---|
| OpenAI Codex, GPT-6 | Drafted earlier report sections; reviewed the code/design; applied the six fixes from AI Review run 1 | Updated `Chicken bomb_D4_Design.md`, `InstrumentSetupService.java`, `ProgramValidator.java`, `ProgramExecutionService.java`, `Main.java`, `ConsoleWarningDecisionPrompt.java`, and this report; current review: `reviews/AI_Review_410aa93.md` | Built with Maven and ran the reported main and alternate CLI flows in fresh working directories |
| Claude Code, Claude Opus 5.5 (`claude-opus-5-5`) | Converted the Phase 1 use case descriptions and the team's Mermaid diagrams into the design file; drafted the use case selection rationale and the revised use case descriptions; aligned the diagrams and the code (sequence diagram notation, class diagram members, order of checks in `ProgramExecutionService`) | `Chicken bomb_D4_Design.md`; `ProgramExecutionService.java` and `SimulatedWeather.java` (commit `58d3edc`) | Parsed every Mermaid diagram with Mermaid 11.4.1; compared the class diagram members with the Java code; built with `mvn package` and ran the main and alternate flows of all four use cases |
| Claude Code, Claude Opus 5.5 (`claude-opus-5-5`) | Drafted section 1; reviewed sections 2–7 against the project description, the design and the code; corrected the inputs, the expected results and the traceability senders; merged the report | `Chicken bomb_D4_Report.md` | Ran every input sequence in sections 2–3 on a fresh build and data directory; checked each traceability row against the sequence diagrams and the sending Java method |
