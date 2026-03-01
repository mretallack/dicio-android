# Requirements: Fix Home Assistant Tests

## Problem Statement

Home Assistant skill tests are failing with `NotImplementedError` because `MockSkillContext` doesn't provide the required `standardMatchHelper` implementation needed by the new API.

## User Stories

### Test Execution
**WHEN** a developer runs `./gradlew testDebugUnitTest`  
**THE SYSTEM SHALL** execute all Home Assistant tests successfully without `NotImplementedError`

### Sentence Parsing Tests
**WHEN** a test calls `Sentences.HomeAssistant["en"]!!.score(ctx, input)`  
**THE SYSTEM SHALL** return a valid score and parsed input data

### Mock Context Behavior
**WHEN** tests use a mock SkillContext  
**THE SYSTEM SHALL** provide a functional `standardMatchHelper` that enables sentence matching

### Test Isolation
**WHEN** tests run  
**THE SYSTEM SHALL** not require actual Android context or external dependencies

## Acceptance Criteria

1. All 38 failing Home Assistant tests pass
2. Tests for `HomeAssistantSkillTest.kt` execute without errors
3. Tests for `SelectSourceIntegrationTest.kt` execute without errors
4. Mock implementation provides minimal required functionality
5. No changes to production code required
6. Tests remain fast and isolated

## Current Failures

- 24 tests in `HomeAssistantSkillTest.kt` failing
- 14 tests in `SelectSourceIntegrationTest.kt` failing
- All failures: `kotlin.NotImplementedError` at score() call
- Root cause: `MockSkillContext.standardMatchHelper` throws `NotImplementedError`

## Constraints

- Must work with existing Kotest framework
- Must be compatible with Dicio's sentence parsing system
- Should follow existing test patterns in the codebase
- Minimal code changes preferred
