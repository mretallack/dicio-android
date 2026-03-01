# Tasks: Fix Home Assistant Tests

## Implementation Tasks

- [x] Create `TestSkillContext.kt` with minimal SkillContext implementation
- [x] Update `HomeAssistantSkillTest.kt` to use `TestSkillContext`
- [x] Update `SelectSourceIntegrationTest.kt` to use `TestSkillContext`
- [x] Run tests and verify all 38 tests pass

## Verification

### Test Results
- ✅ HomeAssistantSkillTest: 24 tests passed, 0 failures
- ✅ SelectSourceIntegrationTest: 14 tests passed, 0 failures
- ✅ Total: 38 tests fixed and passing

### Build Status
- ✅ `./gradlew :app:testDebugUnitTest` - BUILD SUCCESSFUL
- ✅ No compilation errors
- ✅ Test execution time: ~0.2 seconds (fast)

## Files Modified

1. **Created**: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/TestSkillContext.kt`
   - Minimal SkillContext implementation for tests
   - Provides `MatchHelper` with input string
   - Other properties throw NotImplementedError (not used in tests)

2. **Updated**: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkillTest.kt`
   - Removed import of `MockSkillContext`
   - Changed pattern from `data.score(MockSkillContext, "input")` 
   - To: `val input = "..."; data.score(TestSkillContext(input), input)`
   - Applied to all 24 test cases

3. **Updated**: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`
   - Removed import of `MockSkillContext`
   - Applied same pattern change to all 14 test cases

## Implementation Notes

- Solution follows existing patterns from `skill` module tests
- No production code changes required
- Tests remain isolated and fast
- `ParserFormatter` set to `null` (not needed for basic sentence matching)
- `MatchHelper` created with user input for sentence parsing

## Success Criteria Met

✅ All 38 failing Home Assistant tests now pass  
✅ Tests for `HomeAssistantSkillTest.kt` execute without errors  
✅ Tests for `SelectSourceIntegrationTest.kt` execute without errors  
✅ Mock implementation provides minimal required functionality  
✅ No changes to production code required  
✅ Tests remain fast and isolated
