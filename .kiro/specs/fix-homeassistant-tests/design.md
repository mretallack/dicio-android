# Design: Fix Home Assistant Tests

## Overview

The tests fail because `MockSkillContext.standardMatchHelper` returns `null`, but the sentence scoring system requires a valid `MatchHelper` instance. The solution is to create a test-specific `SkillContext` that provides a functional `MatchHelper`.

## Root Cause Analysis

1. **API Change**: Upstream changed `StandardRecognizerData.score()` signature from `score(input: String)` to `score(ctx: SkillContext, input: String)`
2. **Missing Implementation**: `MockSkillContext` in `app/src/test/.../Mocks.kt` has `standardMatchHelper = null`
3. **Requirement**: `StandardRecognizerData.score()` calls `ctx.standardMatchHelper!!` expecting non-null value
4. **Test Failure**: Accessing null `standardMatchHelper` causes `NotImplementedError` from `mocked()` function

## Solution Architecture

### Approach: Create TestSkillContext

Create a minimal test implementation that provides only what's needed for sentence parsing tests.

```
TestSkillContext
├── standardMatchHelper: MatchHelper (created on-demand)
├── parserFormatter: null (not needed for basic sentence parsing)
└── Other properties: throw NotImplementedError (not used in tests)
```

### Key Components

#### 1. TestSkillContext Class
- Location: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/TestSkillContext.kt`
- Purpose: Provide minimal SkillContext for sentence parsing tests
- Implementation:
  ```kotlin
  class TestSkillContext(private val input: String) : SkillContext {
      override var standardMatchHelper: MatchHelper? = MatchHelper(null, input)
      override val parserFormatter: ParserFormatter? = null
      // Other properties throw NotImplementedError
  }
  ```

#### 2. Test Updates
- Replace `MockSkillContext` with `TestSkillContext(input)` in test files
- Pattern: `data.score(TestSkillContext(input), input)`

## Implementation Details

### MatchHelper Requirements

From `skill/src/main/java/org/dicio/skill/standard/util/MatchHelper.kt`:
- Constructor: `MatchHelper(parserFormatter: ParserFormatter?, userInput: String)`
- `parserFormatter` can be `null` for basic word matching
- `userInput` is the sentence being parsed

### Test Pattern

**Before:**
```kotlin
val (score, inputData) = data.score("turn kitchen radio on")
```

**After:**
```kotlin
val input = "turn kitchen radio on"
val (score, inputData) = data.score(TestSkillContext(input), input)
```

### Files to Modify

1. **Create**: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/TestSkillContext.kt`
2. **Update**: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkillTest.kt`
3. **Update**: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`

## Alternative Approaches Considered

### Alternative 1: Modify MockSkillContext
**Rejected**: Would affect other tests that expect `mocked()` behavior

### Alternative 2: Use skill module's MockSkillContext
**Rejected**: Located in different module, would require dependency changes

### Alternative 3: Mock MatchHelper directly
**Rejected**: More complex, requires understanding internal MatchHelper behavior

## Testing Strategy

### Validation Steps
1. Run `./gradlew testDebugUnitTest`
2. Verify all 38 Home Assistant tests pass
3. Verify no other tests are affected
4. Check test execution time remains fast

### Success Criteria
- ✅ All tests in `HomeAssistantSkillTest.kt` pass
- ✅ All tests in `SelectSourceIntegrationTest.kt` pass
- ✅ No changes to production code
- ✅ Test execution time < 5 seconds

## Dependencies

- `org.dicio.skill.context.SkillContext` (interface)
- `org.dicio.skill.standard.util.MatchHelper` (data class)
- `org.dicio.numbers.ParserFormatter` (interface, can be null)

## Risk Assessment

**Low Risk**: 
- Changes isolated to test code
- No production code modifications
- Follows existing patterns from skill module tests
- Easy to revert if issues arise

## Future Considerations

If more tests need similar functionality, consider:
1. Moving `TestSkillContext` to `app/src/test/kotlin/org/stypox/dicio/TestUtils.kt`
2. Creating a test utility function: `fun testContext(input: String): SkillContext`
3. Documenting pattern in test guidelines
