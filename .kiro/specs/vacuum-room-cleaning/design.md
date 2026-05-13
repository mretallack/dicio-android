# Design: Vacuum Room Cleaning Voice Command

## Overview

Extend the Home Assistant skill to support vacuum-specific voice commands. This adds new sentence patterns that are matched by the existing `StandardRecognizerSkill` infrastructure, and new handler methods in `HomeAssistantSkill` that call vacuum-specific HA services.

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Voice Input                            │
│         "vacuum the kitchen and hallway"                 │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│           StandardRecognizerSkill                         │
│   Matches sentence pattern → HomeAssistant.VacuumRoom    │
│   Captures: room_names = "kitchen and hallway"           │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              HomeAssistantSkill                           │
│   handleVacuumRoom(settings, roomNames)                  │
│   1. Find vacuum entity from settings                    │
│   2. Fetch vacuum state → extract rooms attribute        │
│   3. Parse room names, fuzzy match to room list          │
│   4. Call dreame_vacuum.vacuum_clean_segment             │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              HomeAssistantApi                             │
│   callService(baseUrl, token,                            │
│     "dreame_vacuum", "vacuum_clean_segment",             │
│     entityId, {"segments": [2, 1]})                      │
└─────────────────────────────────────────────────────────┘
```

### Sequence Diagram: Vacuum Room Command

```
User          STT          Recognizer       Skill           HA API          HA Server
 │             │               │              │               │                │
 │─"vacuum    │               │              │               │                │
 │  kitchen"─▶│               │              │               │                │
 │             │──text────────▶│              │               │                │
 │             │               │──VacuumRoom──▶│              │                │
 │             │               │  (rooms=     │               │                │
 │             │               │   "kitchen") │               │                │
 │             │               │              │──GET states/  │                │
 │             │               │              │  vacuum.*─────▶│───────────────▶│
 │             │               │              │◀──{rooms:...}─│◀───────────────│
 │             │               │              │               │                │
 │             │               │              │──match "kitchen" to id=2       │
 │             │               │              │               │                │
 │             │               │              │──POST services/│                │
 │             │               │              │  dreame_vacuum/│                │
 │             │               │              │  vacuum_clean_ │                │
 │             │               │              │  segment──────▶│───────────────▶│
 │             │               │              │◀──200 OK──────│◀───────────────│
 │             │               │              │               │                │
 │◀────────────────────────────────"Vacuuming │               │                │
 │             │               │    Kitchen"  │               │                │
```

## Detailed Design

### 1. New Sentence Patterns (`en/home_assistant.yml`)

```yaml
vacuum_room:
  - vacuum|clean|hoover the? .room_names.
  - vacuum|clean|hoover the? .room_names. and .room_names2.

vacuum_start:
  - start the? vacuum|hoover|robot

vacuum_stop:
  - stop the? vacuum|hoover|robot

vacuum_pause:
  - pause the? vacuum|hoover|robot

vacuum_dock:
  - send the? vacuum|hoover|robot home|back|to dock
  - dock the? vacuum|hoover|robot
  - return the? vacuum|hoover|robot to? base|dock|home
```

The `vacuum_room` pattern captures one or two room name groups. For more than two rooms, the user can issue multiple commands or we can extend later.

### 2. New Enum Variants in `HomeAssistant`

The sentences compiler will generate new variants:
- `HomeAssistant.VacuumRoom` — with `roomNames: String?` and optionally `roomNames2: String?`
- `HomeAssistant.VacuumStart`
- `HomeAssistant.VacuumStop`
- `HomeAssistant.VacuumPause`
- `HomeAssistant.VacuumDock`

### 3. Vacuum Entity Discovery

The skill needs to know which entity is the vacuum. Options:
- **Option A:** Use the existing entity mappings — user adds `vacuum.kitchenrobot` with friendly name "vacuum"
- **Option B:** Auto-discover by scanning entity mappings for entities in the `vacuum` domain

**Decision: Option A** — consistent with existing design. The user must have a vacuum entity in their entity mappings. The skill finds it by filtering `entityMappingsList` for entities starting with `vacuum.`.

### 4. Room Name Resolution

```kotlin
private suspend fun resolveRoomIds(
    settings: SkillSettingsHomeAssistant,
    vacuumEntityId: String,
    spokenRoomNames: List<String>
): List<Pair<Int, String>> {
    // 1. GET /api/states/{vacuumEntityId}
    // 2. Extract attributes.rooms → Map<String, List<Room>>
    // 3. Get rooms for selected map (or first map)
    // 4. For each spoken name, fuzzy match against room names
    // 5. Return list of (segmentId, matchedName) pairs
}
```

Fuzzy matching reuses the existing `StringUtils.customStringDistance` approach from `findBestSourceMatch`.

### 5. API Call Modification

`HomeAssistantApi.callService` currently accepts `extraParams: Map<String, String>`. For vacuum segments, we need to pass a list of integers. We need to extend the API to support `Map<String, Any>` or add an overload that accepts a JSON body directly.

**Decision:** Add an overload that accepts a `JSONObject` body directly:

```kotlin
suspend fun callServiceWithBody(
    baseUrl: String,
    token: String,
    domain: String,
    service: String,
    body: JSONObject
): JSONArray
```

The vacuum call would be:
```kotlin
val body = JSONObject()
    .put("entity_id", vacuumEntityId)
    .put("segments", JSONArray(segmentIds))

HomeAssistantApi.callServiceWithBody(
    settings.baseUrl, settings.accessToken,
    "dreame_vacuum", "vacuum_clean_segment", body
)
```

### 6. Output Types

New `HomeAssistantOutput` variants:
- `VacuumRoomSuccess(rooms: List<String>)` — "Vacuuming Kitchen and Hallway"
- `VacuumCommandSuccess(action: String)` — "Vacuum started/stopped/paused/docked"
- `VacuumRoomNotFound(spokenName: String, availableRooms: List<String>)` — "Room not found. Available: Kitchen, Hallway, ..."
- `VacuumNotConfigured()` — "No vacuum entity configured"

### 7. Error Handling

| Scenario | Behaviour |
|----------|-----------|
| No vacuum entity in mappings | Show "No vacuum configured" message |
| Vacuum entity state unreachable | Show connection error |
| Room name doesn't match | Show available rooms |
| Vacuum service call fails | Show error from HA |
| Vacuum has no rooms attribute | Show "Vacuum has no room map" |

## Testing Strategy

Tests follow the existing pattern: Kotest `StringSpec` style, located in `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/`.

### Test File: `VacuumCommandTest.kt`

Sentence recognition tests using `Sentences.HomeAssistant["en"]!!.score()`:

```kotlin
class VacuumCommandTest : StringSpec({

    // --- Sentence Recognition Tests ---

    "parse 'vacuum the kitchen'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "vacuum the kitchen"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "kitchen"
    }

    "parse 'clean the living room'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "clean the living room"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "living room"
    }

    "parse 'hoover the hallway'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "hoover the hallway"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "hallway"
    }

    "parse 'vacuum kitchen and hallway'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "vacuum kitchen and hallway"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "kitchen"
        vacuum.roomNames2?.trim() shouldBe "hallway"
    }

    "parse 'stop the vacuum'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "stop the vacuum"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumStop>()
    }

    "parse 'send the robot home'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "send the robot home"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumDock>()
    }

    "parse 'start the vacuum'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "start the vacuum"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumStart>()
    }

    "parse 'pause the hoover'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "pause the hoover"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumPause>()
    }

    // --- Non-conflict tests ---

    "does not conflict with set_state_on for 'turn vacuum on'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn vacuum on"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        // Should match SetStateOn, not VacuumStart
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
    }
})
```

### Test File: `VacuumRoomMatchingTest.kt`

Room name fuzzy matching tests using reflection (same pattern as `FuzzyMatchingTest.kt`):

```kotlin
class VacuumRoomMatchingTest : StringSpec({
    val rooms = listOf(
        RoomInfo(1, "Hallway"),
        RoomInfo(2, "Kitchen"),
        RoomInfo(3, "Utility Room"),
        RoomInfo(4, "Living Room"),
    )

    // Access private resolveRoomNames via reflection
    val skill = HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant["en"]!!)
    val matchRoom = skill.javaClass.getDeclaredMethod(
        "matchRoomName",
        String::class.java,
        List::class.java
    ).apply { isAccessible = true }

    fun findRoom(spoken: String): RoomInfo? {
        return matchRoom.invoke(skill, spoken, rooms) as RoomInfo?
    }

    // Exact matches
    "exact match - Kitchen" {
        findRoom("kitchen")?.name shouldBe "Kitchen"
        findRoom("kitchen")?.id shouldBe 2
    }

    "exact match - Living Room" {
        findRoom("living room")?.name shouldBe "Living Room"
        findRoom("living room")?.id shouldBe 4
    }

    // Fuzzy matches
    "fuzzy match - 'utility' matches 'Utility Room'" {
        findRoom("utility")?.name shouldBe "Utility Room"
    }

    "fuzzy match - 'hall' matches 'Hallway'" {
        findRoom("hall")?.name shouldBe "Hallway"
    }

    // No match
    "no match - 'bedroom'" {
        findRoom("bedroom") shouldBe null
    }

    "no match - 'garage'" {
        findRoom("garage") shouldBe null
    }
})
```

### Test Execution

Tests run via:
```bash
./gradlew test --tests "org.stypox.dicio.skills.homeassistant.VacuumCommandTest"
./gradlew test --tests "org.stypox.dicio.skills.homeassistant.VacuumRoomMatchingTest"
```

## Implementation Considerations

- The `dreame_vacuum.vacuum_clean_segment` service is Dreame-specific. Other vacuum brands use different services. For now, we target Dreame but structure the code so other integrations can be added later.
- Room data is fetched on each command (not cached) to handle map changes.
- The sentence patterns use generic terms (vacuum/clean/hoover) to support natural phrasing.
- Multi-room support is limited to 2 rooms in a single utterance via sentence patterns, but the underlying code supports any number of segments.
