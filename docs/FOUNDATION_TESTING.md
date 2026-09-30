# LocalAgent Foundation Testing Strategy

## Overview
Phase 2.4 introduces a data-driven sequential Test Case Registry (`FoundationTestRegistry`) and interactive UI Test Runner to execute automated, physical, negative, and hardware verification tests.

## Test Case Registry Structure
- **Model**: `TestCase`
- **Fields**: `id`, `phase`, `category`, `name`, `description`, `command`, `expectedResult`, `testType`, `requiresPhysicalVerification`, `requiredPermission`, `requiredCapability`, `destructiveOrSafe`, `timeout`, `status`, `observedResult`, `error`, `timestamp`, `duration`, `evidenceReferences`.
- **Test Types**: `AUTOMATED`, `PHYSICAL`, `PERMISSION`, `HARDWARE`, `SENSOR`, `SAFETY`, `UI`, `NEGATIVE`.
- **Statuses**: `PENDING`, `RUNNING`, `PASSED`, `FAILED`, `BLOCKED`, `SKIPPED`.

## Sequential Test Runner Workflow
1. Navigate sequential tests via `PREV` and `NEXT`.
2. Inspect exact command, expected behavior, and required permissions.
3. Click `EXECUTE`:
   - Automated tests execute through `GoalDispatcherImpl` -> `ExecutionController` -> Skill/Controller and evaluate PASS/FAIL automatically.
   - Physical tests execute action, update state to `RUNNING` or `OBSERVE`, and await user verification button (`PASS`, `FAIL`, `BLOCKED`).
4. Click `EVIDENCE` to capture view screenshot and save JSON result record to `context.filesDir/evidence/yyyy-MM-dd/TEST-id/`.
5. Results persist across screen navigation and Activity recreations via `TestResultStore`.

## Automated Batch Execution
Click `RUN ALL AUTOMATED TESTS` to execute all non-physical automated, safety, and negative test cases in a single batch pass.
