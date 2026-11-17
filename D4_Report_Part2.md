# D4 Report — Part 2 (How to Run and Use Case Execution)

> Draft contribution for the team report. This section is based on the Java source and use case descriptions in `Chicken bomb_D4_Design.md` at repository commit `c1a1f58` (the checked-out HEAD when this draft was prepared). The program uses local serialized files under `ocs/data/` and simulated instrument, weather, and telescope adapters. The CLI has no login step; actor authentication in the use case preconditions is not implemented in this prototype.

## 2. How to Run

### Requirements and setup

- Install a JDK that supports Java 17 and Apache Maven.
- Open a terminal and change to the `ocs/` directory in the project.
- No external service or database configuration is required. On first launch, the application creates `data/science-plans.ser` with sample science plan `SP-01`, programs `OP-01` and `OP-02`, and a `WARN-01` soft-error warning on `OP-01`. It also creates `data/programs.ser` for submitted programs. These files are relative to the current working directory, so run the application from `ocs/`.

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

Expected output, omitting the generated warning text's prompt formatting:

```text
Submitted
Instrument ready
Soft error for OP-01 [WARN-01]: High airmass may reduce image quality
Observer ID: Proceed with observation? (y/n): Started: OP-01
```

The warning decision and audit action are persisted. On a later run, the decision remains resolved; a previous `PROCEED` allows validation without prompting again, while a previous `STOP` causes a subsequent start attempt to be rejected. Program and science plan state persists in `ocs/data/` between runs.

## 3. Use Case Execution

The examples below use the sample records created by `FileSciencePlanRepository`. Start with a fresh application data directory for the first-run sample state. `OP-01` is valid and has required configuration `GMOS:R400:r`; `OP-02` has no configuration and therefore fails submission validation. The application does not provide a reset command. Because persisted state affects subsequent runs, run state-dependent examples in a fresh copy of the project or restore the initial data before repeating them.

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
| Program has incomplete/invalid required information (4a) | `submit SP-01 OP-02` | `Submission failed`; `OP-02` is not submitted because its configuration is missing |

### UC-05 — Configure Planned Instrument Setup

**Main flow:** The operator requests setup. The application retrieves the program and required configuration, compares it with the instrument's current configuration, applies it when different, reads it back, marks the configuration ready, saves the program, and reports success. The simulated instrument starts without a configuration, so the first successful setup applies `GMOS:R400:r`.

**Test input:** First submit `OP-01`, then run:

```text
setup OP-01
```

**Expected result:** `Instrument ready`; the configuration is marked ready and saved.

**Alternative flows:**

| Case | Input | Expected result |
|---|---|---|
| Program not found (2a) | `setup OP-404` | `Setup failed` |
| Current configuration already matches (3a) | Run `setup OP-01` again in the same running process after successful setup | Setup skips applying a new configuration, verifies the current configuration, and reports `Instrument ready` |
| Verification unsuccessful (5a) | Not reproducible through the current CLI: `SimulatedInstrument.applyConfig` always succeeds and stores the requested configuration | The failure branch exists in `InstrumentSetupService`, but this prototype offers no CLI input or adapter setting to force a verification mismatch |

The design's “not submitted” alternative is not explicitly checked in the current `InstrumentSetupService`; it only rejects a missing program. With the supplied sample data, `OP-02` has a null configuration and may fail during setup rather than cleanly displaying `Setup failed`. This is a design/implementation gap, not a supported test case.

### UC-03 — Start Observing Program

**Main flow:** The operator starts a submitted program. The application validates its status and required information, resolves warnings, checks instrument readiness, lock status, and weather, asks the telescope to start, then marks the program `Running`, locks it, creates and starts an observation session, persists it, and reports success.

**Test input:** On a fresh state, submit and set up `OP-01`, then start it and proceed at the UC-01 prompt:

```text
submit SP-01 OP-01
setup OP-01
start OP-01
OBSERVER-01
y
```

**Expected result:** After the warning decision is recorded, `Started: OP-01`. In the current simulation, weather is always safe and the telescope always starts successfully.

**Alternative flows:**

| Case | Input / setup | Expected result |
|---|---|---|
| Missing, invalid, or not-submitted program (2a) | `start OP-404`, or `start OP-01` before submitting it | `Start rejected: check submission, instrument, weather, telescope, and warning decisions` |
| Observer declines a warning (2b) | At the warning prompt enter `OBSERVER-01`, then `n` | Stop decision is recorded; start is rejected and the program is not started |
| Instrument not ready (3a) | Submit `OP-01`, then start it without running setup | Start is rejected; run `setup OP-01` and retry |
| Program locked (4a) | Try to start an already started program | Start is rejected; the program remains locked |
| Unsafe weather (5a) | Not reproducible through the current CLI: `SimulatedWeather.checkSafeConditions()` always returns `true` | The rejection branch exists in `ProgramExecutionService`, but no CLI input or adapter setting can simulate unsafe weather |
| Telescope fails to start (6a) | Not reproducible through the current CLI: `SimulatedTelescope.startObservation()` always returns `true` | The rejection branch exists in `ProgramExecutionService`, but no CLI input or adapter setting can simulate telescope failure |

### UC-01 — Confirm Soft-Error Warning

UC-01 is invoked within UC-03 validation by `start <programId>` when an unresolved warning is found; it is not a separate CLI command.

**Main flow:** Start a valid, submitted, instrument-ready `OP-01`. At the warning prompt, enter an observer ID and `y`. The application creates a timestamped `PROCEED` decision, resolves `WARN-01`, records the audit action, saves the program, returns to validation, and continues UC-03.

**Test input:**

```text
start OP-01
OBSERVER-01
y
```

**Expected result:** Warning shown as `Soft error for OP-01 [WARN-01]: High airmass may reduce image quality`, followed by `Started: OP-01` (provided submission and setup were completed first).

**Alternative flows:**

| Case | Input | Expected result |
|---|---|---|
| More than one unresolved warning (2a) | No sample with multiple warnings is provided | Code loops through warnings one by one; each needs an observer ID and decision before validation completes |
| Observer does not proceed (4a) | Enter an observer ID, then `n` | A timestamped `STOP` decision is recorded; start is rejected |
| No observer ID/response (4b) | At the warning prompt provide a blank ID, or end standard input before answering | A `STOP` decision is recorded with `NO_RESPONSE` when the ID is blank or unavailable; start is rejected |

The prompt has no timeout mechanism. Blank ID and end-of-input are the implemented no-response cases.
