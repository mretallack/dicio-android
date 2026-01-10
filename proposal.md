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

**Simple State Control:**
- "Turn [entity] on/off"
- "Switch [entity] on/off"
- "[Action] the [entity]"

**Service Template Commands:**
- "Set [entity] to [value] percent" → Uses light_brightness template
- "Set [entity] volume to [number]" → Uses media_volume template
- "Press [command] on [entity]" → Uses remote_command template
- "Play [playlist] on [entity]" → Uses media_playlist template

**Quick Action Commands:**
- "Movie time" → Executes pre-configured movie scene
- "Good night" → Runs bedtime automation
- "Welcome home" → Activates arrival scene
- "[Custom phrase]" → Any user-defined quick action

### Entity Management

- **Friendly Name Mapping**: Users map spoken names to Home Assistant entity IDs
- **Entity Discovery**: Automatic fetching of available entities from Home Assistant
- **Smart Matching**: Fuzzy matching algorithm finds best entity match from spoken input
- **Domain-Specific Actions**: Intelligent action mapping based on entity domain (e.g., "open" for covers, "unlock" for locks)

### Configuration UI

**Dedicated Home Assistant Settings Page**

The current flat settings structure doesn't scale well for complex Home Assistant configurations. A dedicated settings page provides better organization and user experience:

**Main Settings Screen:**
- Base URL configuration for Home Assistant instance
- Access token management for API authentication
- "Configure Home Assistant" button leading to dedicated settings page

**Dedicated Home Assistant Settings Page:**
- **Entity Management Tab**: Visual entity browser with categories (lights, switches, sensors, etc.)
- **Voice Mappings Tab**: Configure friendly names for voice recognition
- **Service Configurations Tab**: Pre-configure complex service calls with parameters
- **Quick Actions Tab**: Set up common automation shortcuts

**Enhanced Entity Management:**
- Live entity discovery with automatic categorization by domain
- Bulk import of entities with suggested friendly names
- Entity status preview and testing
- Search and filter capabilities
- Domain-specific configuration options

**Service Call Configuration:**
- Template system for complex service calls with parameters
- Parameter validation and type checking
- Test service calls directly from settings
- Save frequently used service configurations as voice shortcuts
- Support for dynamic parameters (e.g., volume levels, colors, temperatures)

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

## Enhanced Settings Architecture

### Current Limitations

The existing settings system has several scalability issues:
- Flat entity mapping structure doesn't handle complex service parameters
- No support for service call templates or parameter validation
- Limited organization for users with many entities
- No way to pre-configure complex service calls with multiple parameters

### Proposed Settings Structure

**Updated Protocol Buffer Schema:**

```protobuf
message SkillSettingsHomeAssistant {
  string base_url = 1;
  string access_token = 2;
  repeated EntityMapping entity_mappings = 3;
  repeated ServiceTemplate service_templates = 4;
  repeated QuickAction quick_actions = 5;
  EntityDisplaySettings display_settings = 6;
}

message EntityMapping {
  string friendly_name = 1;
  string entity_id = 2;
  string domain = 3;
  bool enabled = 4;
  repeated string aliases = 5;
}

message ServiceTemplate {
  string name = 1;
  string friendly_name = 2;
  string domain = 3;
  string service = 4;
  repeated ServiceParameter parameters = 5;
  bool requires_entity = 6;
}

message ServiceParameter {
  string key = 1;
  string value_type = 2; // "string", "number", "boolean", "dynamic"
  string default_value = 3;
  bool required = 4;
  string description = 5;
}

message QuickAction {
  string name = 1;
  string voice_trigger = 2;
  string service_template_name = 3;
  string target_entity = 4;
  map<string, string> parameter_values = 5;
}

message EntityDisplaySettings {
  repeated string hidden_domains = 1;
  string sort_order = 2; // "alphabetical", "domain", "recent"
  bool show_unavailable = 3;
}
```

### Three-Tier Command System

**1. Simple State Control (Existing)**
- Direct on/off/toggle commands
- Status queries
- Works with current entity mapping system

**2. Service Call Templates**
- Pre-configured service calls with parameter templates
- Examples: "Set living room light to 50% brightness", "Play jazz playlist on kitchen speaker"
- Parameters can be static or dynamic (extracted from voice command)

**3. Quick Actions**
- One-phrase shortcuts for complex operations
- Examples: "Movie time" → dim lights, close blinds, turn on TV, set volume
- Combines multiple service calls into single voice command

## Technical Implementation

### Enhanced Data Flow

1. **User speaks command** → Speech-to-text conversion
2. **Command classification** → Determine if simple state, service template, or quick action
3. **Parameter extraction** → Parse dynamic values from voice input
4. **Entity/template resolution** → Find matching configured entity or service template
5. **API call construction** → Build appropriate Home Assistant REST API request
6. **Response processing** → Generate speech and UI output

### Service Template Processing

**Template Matching:**
- Voice pattern recognition for service templates
- Dynamic parameter extraction (numbers, colors, names)
- Fallback to entity state control if no template matches

**Parameter Handling:**
- Type validation for extracted parameters
- Default value substitution for missing optional parameters
- Error handling for invalid or missing required parameters

**Example Service Templates:**

```yaml
# Media control template
- name: "media_volume"
  friendly_name: "Set Volume"
  domain: "media_player"
  service: "volume_set"
  parameters:
    - key: "volume_level"
      value_type: "number"
      required: true
      description: "Volume level (0.0-1.0)"

# Light brightness template  
- name: "light_brightness"
  friendly_name: "Set Brightness"
  domain: "light"
  service: "turn_on"
  parameters:
    - key: "brightness_pct"
      value_type: "number"
      required: true
      description: "Brightness percentage (0-100)"

# Remote control template
- name: "remote_command"
  friendly_name: "Remote Control"
  domain: "remote"
  service: "send_command"
  parameters:
    - key: "command"
      value_type: "string"
      required: true
      description: "Remote control command"
```

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
2. **Scalable Configuration**: Dedicated settings page handles complex configurations without cluttering main settings
3. **Flexible Service Calls**: Template system supports any Home Assistant service with proper parameter handling
4. **User-Friendly Setup**: Intuitive configuration with entity discovery, service templates, and quick actions
5. **Comprehensive Coverage**: Supports simple controls, complex service calls, and multi-step automations
6. **Robust Error Handling**: Comprehensive validation and graceful degradation
7. **Performance Optimized**: Efficient caching and batch operations for large entity lists

## Recent Enhancements

- **Service Calls with Data**: Execute Home Assistant services with parameters (e.g., remote commands, media controls)
- **Remote Control Support**: Voice control for Android TV, media players, and other remote-controlled devices
- **Flexible Command Parsing**: Natural language processing for various command formats

## Future Enhancements

- **Advanced Parameter Types**: Support for color values, time ranges, and complex data structures
- **Conditional Logic**: Template parameters based on entity state or time conditions
- **Batch Operations**: Execute multiple service calls in sequence or parallel
- **Scene Integration**: Direct voice control for Home Assistant scenes and scripts
- **Conversation API**: Integration with Home Assistant's native conversation system
- **Machine Learning**: Adaptive parameter suggestions based on usage patterns
- **Voice Feedback**: Customizable response templates for different service call results
- **Integration Testing**: Built-in connectivity and service call testing tools