package org.stypox.dicio.skills.homeassistant

import org.json.JSONObject
import java.util.regex.Pattern

class ServiceTemplateManager {
    
    data class ParameterMatch(
        val key: String,
        val value: String,
        val type: String
    )
    
    data class TemplateMatch(
        val template: ServiceTemplate,
        val parameters: Map<String, String>,
        val entityId: String?
    )
    
    fun findMatchingTemplate(
        input: String,
        templates: List<ServiceTemplate>,
        entityMappings: List<EntityMapping>
    ): TemplateMatch? {
        
        // Try to match service templates
        for (template in templates) {
            val match = matchTemplate(input, template, entityMappings)
            if (match != null) return match
        }
        
        return null
    }
    
    private fun matchTemplate(
        input: String,
        template: ServiceTemplate,
        entityMappings: List<EntityMapping>
    ): TemplateMatch? {
        
        // Extract entity if required
        val entityId = if (template.requiresEntity) {
            findEntityInInput(input, entityMappings) ?: return null
        } else null
        
        // Extract parameters based on template type
        val parameters = when (template.name) {
            "light_brightness" -> extractBrightnessParameters(input)
            "media_volume" -> extractVolumeParameters(input)
            "remote_command" -> extractRemoteParameters(input)
            else -> emptyMap()
        }
        
        // Validate required parameters
        val requiredParams = template.parametersList.filter { it.required }
        if (requiredParams.any { param -> !parameters.containsKey(param.key) }) {
            return null
        }
        
        return TemplateMatch(template, parameters, entityId)
    }
    
    private fun findEntityInInput(input: String, entityMappings: List<EntityMapping>): String? {
        for (mapping in entityMappings) {
            if (input.contains(mapping.friendlyName, ignoreCase = true)) {
                return mapping.entityId
            }
            for (alias in mapping.aliasesList) {
                if (input.contains(alias, ignoreCase = true)) {
                    return mapping.entityId
                }
            }
        }
        return null
    }
    
    private fun extractBrightnessParameters(input: String): Map<String, String> {
        val pattern = Pattern.compile("(\\d+)\\s*(?:percent|%)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(input)
        
        return if (matcher.find()) {
            mapOf("brightness_pct" to matcher.group(1))
        } else emptyMap()
    }
    
    private fun extractVolumeParameters(input: String): Map<String, String> {
        val pattern = Pattern.compile("(?:volume\\s+(?:to\\s+)?)?(\\d+)(?:\\s*(?:percent|%))?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(input)
        
        return if (matcher.find()) {
            val volume = matcher.group(1)?.toInt() ?: 0
            mapOf("volume_level" to (volume / 100.0).toString())
        } else emptyMap()
    }
    
    private fun extractRemoteParameters(input: String): Map<String, String> {
        val commands = listOf("play", "pause", "stop", "up", "down", "left", "right", 
                             "select", "back", "home", "menu", "power", "volume_up", "volume_down")
        
        for (command in commands) {
            if (input.contains(command, ignoreCase = true)) {
                return mapOf("command" to command.uppercase())
            }
        }
        
        return emptyMap()
    }
    
    fun getDefaultTemplates(): List<ServiceTemplate> {
        return listOf(
            ServiceTemplate.newBuilder()
                .setName("light_brightness")
                .setFriendlyName("Set Light Brightness")
                .setDomain("light")
                .setService("turn_on")
                .setRequiresEntity(true)
                .addParameters(
                    ServiceParameter.newBuilder()
                        .setKey("brightness_pct")
                        .setValueType("number")
                        .setRequired(true)
                        .setDescription("Brightness percentage (0-100)")
                        .build()
                )
                .build(),
                
            ServiceTemplate.newBuilder()
                .setName("media_volume")
                .setFriendlyName("Set Media Volume")
                .setDomain("media_player")
                .setService("volume_set")
                .setRequiresEntity(true)
                .addParameters(
                    ServiceParameter.newBuilder()
                        .setKey("volume_level")
                        .setValueType("number")
                        .setRequired(true)
                        .setDescription("Volume level (0.0-1.0)")
                        .build()
                )
                .build(),
                
            ServiceTemplate.newBuilder()
                .setName("remote_command")
                .setFriendlyName("Remote Control")
                .setDomain("remote")
                .setService("send_command")
                .setRequiresEntity(true)
                .addParameters(
                    ServiceParameter.newBuilder()
                        .setKey("command")
                        .setValueType("string")
                        .setRequired(true)
                        .setDescription("Remote control command")
                        .build()
                )
                .build()
        )
    }
}
