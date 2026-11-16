# Preliminary AI Review — Chicken bomb D4

- Review date: 2026-11-17
- Tool/model: OpenAI Codex, GPT-6 (the exact build/version identifier was not exposed in this session)
- Commit reviewed: `58d3edc9532bd52df5fa5266dc9786ec6ab1367b` (`Add D4 design file and align code with design`)
- File access: The design and Java source were read from the checked-out local repository workspace.
- Scope: Preliminary comparison of `Chicken bomb_D4_Design.md` with Java files under `ocs/src/main/java`, following the intent of the project's Appendix A review prompt. This is not a review of a later/final commit.

## Findings

### 1. UC-05 handling of a program that is not submitted or has incomplete data

**Evidence:** The UC-05 alternative flow in `Chicken bomb_D4_Design.md` §4.2 says that if the program does not exist or has not been submitted, the system displays `Setup failed`, while its preconditions also require that the program has already been submitted. `InstrumentSetupService.setupInstrument` checks whether `findById` returned `null`, but does not explicitly check that the returned program's status is `Submitted`. Under the current normal lifecycle, the submission service saves programs into the observing-program repository only after successful submission, so a not-yet-submitted program is normally absent and setup returns failure. The rule is therefore implicit in repository state rather than checked as a program-state condition.

**Impact:** The use case description contains a precondition/alternative-flow inconsistency, and the implementation relies on repository lifecycle to enforce the submitted-state rule. If a non-submitted program is ever stored in the repository by another path, setup may proceed when it has a configuration. The current evidence does not show that this happens through the normal CLI flow.

**Suggested resolution:** Decide the intended rule with the team. If UC-05 should reject a stored program unless its status is `Submitted`, add an explicit guard in `InstrumentSetupService.setupInstrument` and show the status check in the sequence diagram. Also reconcile the use case precondition with alternative 2a. If the precondition is intended to exclude unsubmitted programs, remove or reframe the unreachable alternative. Record the chosen resolution in the final report and verify it against the final code.

## Review limitations

This review did not run the application, execute a test suite, or run SonarQube. No claims about runtime test results or SonarQube findings are made. Rerun the required AI review on the final submitted commit and append or replace this review record as appropriate.
