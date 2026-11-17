# D4 Report — Part 1 (Design Rationale)

> Draft based on `Chicken bomb_D4_Design.md` and the Java implementation in `ocs/src/main/java`. The design uses a small layered structure with application services, domain objects, ports, and file/simulation adapters. This rationale describes the current prototype, including its trade-offs.

## 1. Design Rationale

### 1.1 High Cohesion

Each class and package groups code that serves one closely related responsibility. The domain objects hold the observing concepts and state: `ObservingProgram` manages program status, lock state, warnings and sessions; `InstrumentConfiguration` holds the required setup and readiness state; and `SoftErrorWarning` holds a warning and its decision. Application services coordinate one use case each: `ProgramSubmissionService` submits a program, `InstrumentSetupService` prepares its instrument, `ProgramExecutionService` starts an observation, and `WarningConfirmationService` records the observer's response. `ProgramValidator` handles start-time validation.

The adapters also have focused roles. `OcsCli` handles command input and output, `ConsoleWarningDecisionPrompt` handles the interactive warning prompt, file repositories handle persistence, and the simulated gateway classes provide the external instrument, weather and telescope behavior. This keeps use-case coordination out of CLI and storage code, and keeps domain state changes out of the adapters.

The cohesion is not absolute: `Main` intentionally assembles the application's objects and concrete adapters as the composition root. It is the place where wiring belongs, rather than a use-case class.

### 1.2 Low Coupling

Application services depend on interfaces in `ocs.ports` for persistence and external systems. For example, `InstrumentSetupService` depends on `InstrumentGateway`, and `ProgramExecutionService` depends on `WeatherGateway` and `TelescopeGateway`. `ProgramSubmissionService` uses `SciencePlanRepository` and `ObservingProgramRepository`. The file-based and simulated implementations implement those interfaces, so a storage or hardware adapter can be replaced without changing the use-case service's control flow, provided it honors the same interface contract.

Constructor injection makes these dependencies visible and allows `Main` to assemble the application. The CLI also receives its services through its constructor. This avoids service classes creating or locating their own infrastructure dependencies. The design still has coupling through shared domain types, method names, IDs and interface contracts; low coupling means changes are localized where practical, not that classes have no dependencies. The concrete choices are concentrated in `Main`.

### 1.3 Connascence

Connascence describes how changes in one part of a system require coordinated changes elsewhere. This design keeps most coordination local and uses explicit contracts where classes must work together:

- **Connascence of Name:** callers use names such as `findById`, `save`, `startObservation` and `checkSafeConditions`. Renaming an interface method requires its implementations and callers to change. The dependency is explicit in the port interfaces and is limited to the relevant operation.
- **Connascence of Type:** application services and adapters agree on types such as `ObservingProgram`, `WarningDecision`, `InstrumentGateway` and `ObservingProgramRepository`. The interfaces make these types visible at compile time; exchanging concrete storage or gateway implementations does not require changing the service's declared dependency type.
- **Connascence of Position:** constructors and CLI commands pass values in a defined order, for example `submit <planId> <programId>` and the `ProgramSubmissionService` constructor parameters. Reordering same-typed `String` values can be easy to get wrong. The CLI's usage text and distinct parameter names reduce, but do not eliminate, this risk.
- **Connascence of Algorithm / Execution order:** the workflow requires submission before setup and setup before starting. The relevant status and readiness checks are enforced by the use-case services; the command sequence is documented in the run instructions. These are business dependencies between operations, rather than dependencies on a particular file or hardware implementation.
- **Connascence of Identity:** use cases carry a `programId` across the plan, repository and gateway workflow so each operation addresses the same observing program. Keeping ID lookup behind repository interfaces limits the implementation details exposed to callers.

The stronger dependencies are therefore kept at clear boundaries (domain types, ports, and workflow ordering) instead of spreading file paths or device-specific details throughout the application. The simple `String` IDs and configuration values remain a source of weaker type safety in this prototype.

### 1.4 Design Patterns and Class Roles

The architecture follows a ports-and-adapters (hexagonal) approach: application and domain code express what they need through interfaces, while infrastructure classes provide implementations. It also uses Repository, Adapter, Application Service and constructor-based Dependency Injection patterns identified in the design.

| Pattern / role | Classes | Reason and responsibility |
|---|---|---|
| Composition Root | `Main` | Creates concrete repositories, simulated gateways, prompt and application services, then injects them into `OcsCli`. This keeps concrete infrastructure selection in one place. |
| Application Service | `ProgramSubmissionService`, `InstrumentSetupService`, `ProgramExecutionService`, `ProgramValidator`, `WarningConfirmationService` | Coordinates the steps and policy for UC-04, UC-05, UC-03, validation, and UC-01. The services invoke domain behavior and ports rather than reading files or speaking directly to hardware. |
| Entity / Aggregate Root | `SciencePlan`, `ObservingProgram` | `SciencePlan` groups its programs. `ObservingProgram` is the aggregate root for its configuration, warnings and observation sessions, and owns lifecycle operations such as `submit()` and `start()`. |
| Domain Entity / Value Object | `InstrumentConfiguration`, `SoftErrorWarning`, `ObservationSession`, `WarningDecision` | Model instrument readiness, warning resolution, observation session lifecycle and the recorded warning decision. `WarningDecision` is an immutable-in-intent record of outcome, observer ID and timestamp (implemented as a serializable class). |
| Port / Repository | `SciencePlanRepository`, `ObservingProgramRepository`, `AuditRepository` | Define persistence operations without exposing the file format to application services. |
| Repository Adapter | `FileSciencePlanRepository`, `FileObservingProgramRepository`, `FileAuditRepository` | Implement the repository ports using serialized Java object maps for plans/programs and an append-only text file for audit actions. |
| External-system Port / Adapter | `InstrumentGateway`, `TelescopeGateway`, `WeatherGateway`; `SimulatedInstrument`, `SimulatedTelescope`, `SimulatedWeather` | Define the operations needed from observatory systems and provide local simulations for the prototype. A real integration can implement the same ports later. |
| Interaction Port / Adapter | `WarningDecisionPrompt`; `ConsoleWarningDecisionPrompt` | Separates the warning decision workflow from console input/output so another interaction mechanism can be substituted. |
| Constructor-based Dependency Injection | `OcsCli` and application services | Dependencies are passed to constructors, making required collaborators explicit and allowing `Main` to select implementations. |

### 1.5 Data Store Choice

The prototype uses local files instead of a database. `FileSciencePlanRepository` and `FileObservingProgramRepository` serialize maps of domain objects into `data/science-plans.ser` and `data/programs.ser`. `FileAuditRepository` appends audit actions as UTF-8 text to `data/audit.log`. The domain objects stored in the serialized maps implement Java `Serializable`.

This choice keeps the teaching prototype self-contained: there is no database server, driver, schema migration or external setup, and records survive application restarts. Repository interfaces isolate this decision from use-case code, and the separate text audit file is easy to inspect.

The file store is a fit for a single-process demonstration, not a production observatory system. Each repository reads and rewrites the whole map on save, which is simple but does not scale well. There is no transaction spanning program persistence and audit logging, no protection against concurrent writers, and Java object serialization couples stored data to compatible class definitions. The audit log is append-only but currently records a formatted action string rather than a structured event. A production deployment would need a transactional, concurrent data store and a stronger audit design while preserving the repository ports where possible.
