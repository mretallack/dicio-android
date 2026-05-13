# Tasks: Vacuum Room Cleaning Voice Command

## Implementation Plan

### Phase 1: Sentence Patterns & Generated Code

- [ ] **Task 1:** Add vacuum sentence patterns to `app/src/main/sentences/en/home_assistant.yml`
  - Add `vacuum_room`, `vacuum_start`, `vacuum_stop`, `vacuum_pause`, `vacuum_dock` patterns
  - Build to verify sentences compiler generates the new enum variants

### Phase 2: API Layer

- [ ] **Task 2:** Add `callServiceWithBody` overload to `HomeAssistantApi`
  - Accepts a `JSONObject` body directly instead of `Map<String, String>`
  - Needed for passing segment IDs as a JSON array

### Phase 3: Skill Logic

- [ ] **Task 3:** Add vacuum entity discovery helper
  - Find vacuum entity from `entityMappingsList` by domain prefix `vacuum.`
  - Return the entity ID or null if not configured

- [ ] **Task 4:** Add room name resolution logic
  - Fetch vacuum entity state from HA
  - Extract `rooms` attribute and parse room list
  - Fuzzy match spoken room names to room names using `StringUtils.customStringDistance`
  - Return list of matched segment IDs

- [ ] **Task 5:** Add handler methods in `HomeAssistantSkill`
  - `handleVacuumRoom` — resolves rooms, calls `dreame_vacuum.vacuum_clean_segment`
  - `handleVacuumCommand` — handles start/stop/pause/dock commands
  - Wire up new `HomeAssistant.*` enum variants in `generateOutput`

### Phase 4: Output & UI

- [ ] **Task 6:** Add new `HomeAssistantOutput` variants
  - `VacuumRoomSuccess` — confirms rooms being cleaned
  - `VacuumCommandSuccess` — confirms start/stop/pause/dock
  - `VacuumRoomNotFound` — room not matched, shows available rooms
  - `VacuumNotConfigured` — no vacuum entity in mappings
  - Add string resources for each output type

### Phase 5: Configuration

- [ ] **Task 7:** Add vacuum entity to entity mappings on test device
  - Add `vacuum.kitchenrobot` with friendly name "vacuum" to the protobuf settings

### Phase 6: Testing

- [ ] **Task 8:** Add unit tests for room name matching
  - Test fuzzy matching of room names (e.g., "kitchen" → "Kitchen", "living room" → "Living Room")
  - Test multi-room parsing
  - Test no-match scenario

- [ ] **Task 9:** Add unit tests for sentence pattern matching
  - Verify "vacuum the kitchen" matches `VacuumRoom`
  - Verify "stop the vacuum" matches `VacuumStop`
  - Verify "clean the hallway and kitchen" matches with two rooms

- [ ] **Task 10:** Manual end-to-end test on device
  - Build and install on phone
  - Test "vacuum the kitchen" → vacuum starts cleaning kitchen
  - Test "stop the vacuum" → vacuum stops
  - Test "send the vacuum home" → vacuum returns to dock
  - Test invalid room name → shows available rooms

## Dependencies

- Sentences compiler must support the new patterns (it should, as they follow existing conventions)
- Vacuum entity must be in entity mappings
- HA must have `dreame_vacuum` integration installed

## Risks

- Sentence patterns may conflict with existing `set_state_on` patterns (e.g., "turn the vacuum on" should still work via the existing toggle logic)
- Other vacuum brands use different service calls — this implementation is Dreame-specific
- Room names with special characters or very short names may not fuzzy match well
