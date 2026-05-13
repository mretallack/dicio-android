# Requirements: Vacuum Room Cleaning Voice Command

## User Stories

### US-1: Clean a specific room by voice
As a user, I want to say "vacuum the kitchen" so that the robot vacuum cleans a specific room without me needing to open the Home Assistant app.

**Acceptance Criteria:**
- User can say "vacuum the kitchen" and the vacuum starts cleaning the kitchen
- User can say "clean the living room" and the vacuum starts cleaning the living room
- The system matches spoken room names to configured rooms using fuzzy matching
- The system provides spoken/visual feedback confirming which room is being cleaned

### US-2: Clean multiple rooms by voice
As a user, I want to say "vacuum the kitchen and hallway" so that the robot vacuum cleans multiple rooms in one command.

**Acceptance Criteria:**
- User can specify multiple rooms in a single command
- All specified rooms are sent in a single service call
- Feedback confirms all rooms that will be cleaned

### US-3: Basic vacuum control by voice
As a user, I want to say "stop the vacuum" or "send the vacuum home" for basic vacuum control.

**Acceptance Criteria:**
- "stop the vacuum" calls vacuum.stop
- "send the vacuum home" / "dock the vacuum" calls vacuum.return_to_base
- "start the vacuum" calls vacuum.start (full clean)
- "pause the vacuum" calls vacuum.pause

### US-4: Vacuum status by voice
As a user, I want to ask "what is the vacuum doing?" to get the current status.

**Acceptance Criteria:**
- Reports current state (docked, cleaning, returning, paused, etc.)
- Reports current room if segment cleaning
- Reports battery level

## Requirements (EARS Notation)

### Functional Requirements

**REQ-1:** WHEN the user says "vacuum <room_name>" or "clean <room_name>"
THE SYSTEM SHALL call the `dreame_vacuum.vacuum_clean_segment` service with the segment ID corresponding to the spoken room name.

**REQ-2:** WHEN the user specifies multiple rooms (e.g., "vacuum the kitchen and hallway")
THE SYSTEM SHALL call `dreame_vacuum.vacuum_clean_segment` with all matching segment IDs in a single request.

**REQ-3:** WHEN the user says a room name that does not match any configured room
THE SYSTEM SHALL inform the user that the room was not found and list available rooms.

**REQ-4:** WHEN the user says "stop the vacuum" or "pause the vacuum"
THE SYSTEM SHALL call the appropriate vacuum service (vacuum.stop or vacuum.pause).

**REQ-5:** WHEN the user says "send the vacuum home" or "dock the vacuum"
THE SYSTEM SHALL call vacuum.return_to_base.

**REQ-6:** WHEN the user says "start the vacuum"
THE SYSTEM SHALL call vacuum.start for a full clean.

**REQ-7:** WHEN the user asks for vacuum status
THE SYSTEM SHALL retrieve and report the vacuum's current state, battery level, and current room (if cleaning).

**REQ-8:** THE SYSTEM SHALL retrieve room names and segment IDs dynamically from the vacuum entity's `rooms` attribute in Home Assistant.

**REQ-9:** THE SYSTEM SHALL use fuzzy string matching to match spoken room names to configured room names, consistent with existing entity matching behaviour.

### Non-Functional Requirements

**REQ-10:** THE SYSTEM SHALL support the vacuum command in all languages where the Home Assistant skill is available, by adding sentence patterns to the relevant `.yml` files.

**REQ-11:** THE SYSTEM SHALL work with any vacuum entity that has a `rooms` attribute, not just Dreame vacuums (though the initial implementation targets Dreame's `vacuum_clean_segment` service).

## Out of Scope

- Selecting suction level or water volume via voice (can be added later)
- Zone cleaning (arbitrary coordinates)
- Map selection for multi-floor setups
- Scheduling vacuum runs via voice

## Entities and Services

### Vacuum Entity
- Entity ID: `vacuum.kitchenrobot`
- Rooms attribute: `{'Map 1': [{'id': 1, 'name': 'Hallway'}, {'id': 2, 'name': 'Kitchen'}, {'id': 3, 'name': 'Utility Room'}, {'id': 4, 'name': 'Living Room'}]}`

### Services Used
- `dreame_vacuum.vacuum_clean_segment` — segments param (list of room IDs)
- `vacuum.start` — full clean
- `vacuum.stop` — stop cleaning
- `vacuum.pause` — pause cleaning
- `vacuum.return_to_base` — send home
