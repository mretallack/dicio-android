# Home Assistant Integration for Dicio Voice Assistant

## Overview

This proposal describes the implementation of Home Assistant integration in Dicio, enabling voice control of smart home devices through the Home Assistant REST API. The integration allows users to query device status and control entities using natural language commands.

## Architecture

### Core Components

1. **HomeAssistantSkill** - Main skill class that processes voice commands and routes them to appropriate handlers
2. **HomeAssistantApi** - HTTP client for communicating with Home Assistant REST API
3. **HomeAssistantOutput** - Sealed interface defining different response types with speech and UI components
4. **HomeAssistantInfo** - Skill metadata and settings UI configuration
5. **Entity Mapping System** - User-configurable mappings between spoken names and Home Assistant entity IDs

### Sentence Recognition

The skill supports three main command types defined in `skill_definitions.yml`:

- **get_status**: Query entity state ("What's the status of living room light?")
- **set_state**: Control entity state ("Turn living room light on")
- **call_service**: Execute services with data ("Press DPAD_CENTER on living room remote")

Natural language patterns are defined in `home_assistant.yml` with flexible syntax supporting various phrasings.

## Features

### Voice Commands

**Status Queries:**
- "What is the status of [entity]?"
- "Check [entity] status"
- "Is [entity] on or off?"

**State Control:**
- "Turn [entity] on/off"
- "Switch [entity] on/off"
- "[Action] the [entity]"

**Service Calls:**
- "Press [command] on [entity]"
- "Send [command] to [entity]"
- "Execute [command] on [entity]"

### Entity Management

- **Friendly Name Mapping**: Users map spoken names to Home Assistant entity IDs
- **Entity Discovery**: Automatic fetching of available entities from Home Assistant
- **Smart Matching**: Fuzzy matching algorithm finds best entity match from spoken input
- **Domain-Specific Actions**: Intelligent action mapping based on entity domain (e.g., "open" for covers, "unlock" for locks)

### Configuration UI

Comprehensive settings interface built with Jetpack Compose:
- Base URL configuration for Home Assistant instance
- Access token management for API authentication
- Entity mapping editor with add/edit/delete functionality
- Entity picker dialog with live entity discovery
- Persistent storage using Protocol Buffers and DataStore

### Error Handling

Robust error handling with specific user feedback:
- **EntityNotMapped**: When spoken name doesn't match any configured entity
- **EntityNotFound**: When entity ID doesn't exist in Home Assistant
- **InvalidAction**: When action cannot be parsed or applied to entity type
- **ConnectionFailed**: Network connectivity issues
- **AuthFailed**: Authentication/authorization failures

### API Integration

Direct integration with Home Assistant REST API:
- **GET /api/states**: Retrieve all entity states for discovery
- **GET /api/states/{entity_id}**: Get specific entity status
- **POST /api/services/{domain}/{service}**: Execute entity actions
- **POST /api/services/{domain}/{service}** (with data): Execute services with parameters like remote commands

Authentication via Bearer token with proper HTTP headers and JSON content handling.

## Technical Implementation

### Data Flow

1. User speaks command → Speech-to-text conversion
2. Sentence matching → Extract entity name and action
3. Entity resolution → Find matching configured entity
4. API call → Execute Home Assistant REST API request
5. Response processing → Generate speech and UI output

### Action Parsing

Intelligent action parsing with domain-specific mappings:
- Generic actions: "on", "off", "toggle"
- Domain-specific translations (cover: "open"/"close", lock: "unlock"/"lock")
- Flexible input recognition (supports synonyms and variations)

### State Management

- Settings persisted using Protocol Buffers serialization
- Reactive UI updates via Kotlin Flows and Compose State
- Coroutine-based async operations for network calls

## Integration Points

The skill integrates seamlessly with Dicio's architecture:
- Registered in `SkillHandler.allSkillInfoList`
- Follows standard skill interface (`StandardRecognizerSkill`)
- Uses shared `SkillContext` for Android resources and utilities
- Supports Dicio's multilingual sentence system

## Benefits

1. **Privacy-First**: All processing happens on-device except API calls to user's own Home Assistant instance
2. **Flexible Configuration**: Users can customize entity mappings to match their speaking preferences
3. **Comprehensive Coverage**: Supports all Home Assistant entity types through generic action system
4. **User-Friendly**: Intuitive setup with entity discovery and visual configuration
5. **Robust**: Comprehensive error handling and graceful degradation

## Recent Enhancements

- **Service Calls with Data**: Execute Home Assistant services with parameters (e.g., remote commands, media controls)
- **Remote Control Support**: Voice control for Android TV, media players, and other remote-controlled devices
- **Flexible Command Parsing**: Natural language processing for various command formats

## Future Enhancements

- Support for entity attributes and complex state queries
- Batch operations for multiple entities
- Scene and automation triggering
- Integration with Home Assistant's conversation API
- Support for numeric values and dimming controls
- Custom service parameter mapping