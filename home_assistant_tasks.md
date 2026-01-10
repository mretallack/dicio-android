# Home Assistant Integration Tasks

## Protocol Buffer Schema Updates

- [x] Update `skill_settings_home_assistant.proto` with new message types:
  - [x] Add `ServiceTemplate` message
  - [x] Add `ServiceParameter` message  
  - [x] Add `QuickAction` message
  - [x] Add `EntityDisplaySettings` message
  - [x] Update `EntityMapping` with domain, enabled, aliases fields

## Settings UI Refactor

- [ ] Create dedicated Home Assistant settings activity/fragment
- [ ] Implement tabbed interface:
  - [ ] Entity Management tab
  - [ ] Voice Mappings tab
  - [ ] Service Configurations tab
  - [ ] Quick Actions tab
- [ ] Add entity categorization by domain
- [ ] Implement bulk entity import functionality
- [ ] Add search and filter capabilities for entities

## Service Template System

- [x] Create `ServiceTemplateManager` class
- [x] Implement template matching logic
- [x] Add parameter extraction from voice commands
- [x] Create parameter validation system
- [x] Add default service templates (volume, brightness, remote commands)
- [ ] Implement template testing functionality in settings

## Enhanced API Layer

- [x] Extend `HomeAssistantApi` for template-based service calls
- [x] Add parameter type conversion utilities
- [ ] Implement batch service call support
- [x] Add service discovery and validation

## Voice Command Processing

- [x] Update sentence patterns for service templates
- [x] Implement dynamic parameter extraction
- [x] Add command classification logic (simple/template/quick action)
- [x] Create fallback handling for unmatched templates

## Quick Actions System

- [x] Create `QuickActionManager` class
- [x] Implement multi-step action execution
- [x] Add quick action configuration UI
- [x] Create predefined action templates (movie time, good night, etc.)

## Error Handling & Validation

- [x] Add parameter validation with user-friendly error messages
- [ ] Implement service call testing in settings
- [ ] Add connectivity validation for Home Assistant instance
- [x] Create graceful degradation for unavailable services

## Testing & Documentation

- [ ] Update unit tests for new functionality
- [ ] Add integration tests for service templates
- [ ] Update user documentation
- [x] Create migration logic for existing entity mappings
