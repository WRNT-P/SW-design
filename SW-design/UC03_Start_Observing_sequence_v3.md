## Sequence Diagram: UC-03 Start Observing Program

```mermaid
sequenceDiagram
    actor TO as Telescope Operator
    actor SO as Science Observer
    participant cli as :OcsCli
    participant svc as :ProgramExecutionService
    participant val as :ProgramValidator
    participant repo as :ObservingProgramRepository
    participant prog as program:ObservingProgram
    participant warnSvc as :WarningConfirmationService
    participant prompt as :WarningDecisionPrompt
    participant warning as warning:SoftErrorWarning
    participant audit as :AuditRepository
    participant cfg as config:InstrumentConfiguration
    participant wx as :WeatherGateway
    participant tel as :TelescopeGateway
    participant sess as session:ObservationSession

    TO->>cli: startProgram(programId)
    cli->>svc: startProgram(programId)
    svc->>val: validateProgram(programId, warningService)
    val->>repo: findById(programId)
    repo-->>val: program
    val->>prog: validate()
    prog-->>val: valid
    alt 2a: missing, invalid or not submitted
        val-->>svc: false
        svc-->>cli: false
    else valid
        val->>prog: getWarnings()
        prog-->>val: warnings
        loop Each soft error
            val->>warning: isResolved()
            alt warning unresolved
                val->>warnSvc: confirmWarning(programId, warningId)
                warnSvc->>repo: findById(programId)
                repo-->>warnSvc: program
                warnSvc->>prog: getWarning(warningId)
                prog-->>warnSvc: warning
                warnSvc->>prompt: requestDecision(programId, warning)
                prompt-->>SO: Present warning (program + condition)
                SO-->>prompt: proceed or stop, observerId
                prompt-->>warnSvc: decision
                warnSvc->>warning: resolve(decision)
                warnSvc->>audit: logAction(decision, decision maker, timestamp)
                warnSvc->>repo: save(program)
                alt observer proceeds
                    warnSvc-->>val: true
                else observer declines or disconnects
                    warnSvc-->>val: false
                    val-->>svc: false
                    svc-->>cli: false
                end
            end
        end
        val-->>svc: true
        svc->>repo: findById(programId)
        repo-->>svc: program
        svc->>prog: getConfiguration()
        prog-->>svc: config
        svc->>cfg: isInstrumentReady()
        cfg-->>svc: ready
        alt 3a: not ready
            svc-->>cli: false
        else ready
            svc->>prog: isLocked()
            prog-->>svc: locked
            alt 5a: locked
                svc-->>cli: false
            else unlocked
                svc->>wx: checkSafeConditions()
                wx-->>svc: safe
                alt 4a: unsafe
                    svc-->>cli: false
                else safe
                    svc->>tel: startObservation()
                    tel-->>svc: started
                    alt 6a: not started
                        svc-->>cli: false
                    else started
                        svc->>prog: start()
                        create participant sess as session:ObservationSession
                        prog->>sess: «create» ObservationSession(sessionId)
                        prog->>sess: start()
                        svc->>repo: save(program)
                        svc-->>cli: true
                    end
                end
            end
        end
    end
    Note over TO,cli: OcsCli prints the start result
```
