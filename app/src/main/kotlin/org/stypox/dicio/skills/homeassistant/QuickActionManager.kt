package org.stypox.dicio.skills.homeassistant

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class QuickActionManager {
    
    data class ActionResult(
        val success: Boolean,
        val message: String,
        val actionName: String
    )
    
    suspend fun executeQuickAction(
        baseUrl: String,
        token: String,
        quickAction: QuickAction,
        templates: List<ServiceTemplate>,
        entityMappings: List<EntityMapping>
    ): ActionResult = coroutineScope {
        
        try {
            val template = templates.find { it.name == quickAction.serviceTemplateName }
                ?: return@coroutineScope ActionResult(
                    false, 
                    "Service template '${quickAction.serviceTemplateName}' not found",
                    quickAction.name
                )
            
            val entityId = quickAction.targetEntity.ifEmpty { 
                findEntityByName(quickAction.targetEntity, entityMappings)
            }
            
            HomeAssistantApi.executeQuickAction(baseUrl, token, quickAction, template, entityId)
            
            ActionResult(
                true,
                "Successfully executed ${quickAction.name}",
                quickAction.name
            )
            
        } catch (e: Exception) {
            ActionResult(
                false,
                "Failed to execute ${quickAction.name}: ${e.message}",
                quickAction.name
            )
        }
    }
    
    suspend fun executeMultipleActions(
        baseUrl: String,
        token: String,
        actions: List<QuickAction>,
        templates: List<ServiceTemplate>,
        entityMappings: List<EntityMapping>
    ): List<ActionResult> = coroutineScope {
        
        actions.map { action ->
            async {
                executeQuickAction(baseUrl, token, action, templates, entityMappings)
            }
        }.awaitAll()
    }
    
    private fun findEntityByName(name: String, entityMappings: List<EntityMapping>): String? {
        return entityMappings.find { mapping ->
            mapping.friendlyName.equals(name, ignoreCase = true) ||
            mapping.aliasesList.any { it.equals(name, ignoreCase = true) }
        }?.entityId
    }
    
    fun getDefaultQuickActions(): List<QuickAction> {
        return listOf(
            QuickAction.newBuilder()
                .setName("movie_time")
                .setVoiceTrigger("movie time")
                .setServiceTemplateName("light_brightness")
                .setTargetEntity("light.living_room")
                .putParameterValues("brightness_pct", "20")
                .build(),
                
            QuickAction.newBuilder()
                .setName("good_night")
                .setVoiceTrigger("good night")
                .setServiceTemplateName("light_brightness")
                .setTargetEntity("light.bedroom")
                .putParameterValues("brightness_pct", "0")
                .build(),
                
            QuickAction.newBuilder()
                .setName("welcome_home")
                .setVoiceTrigger("welcome home")
                .setServiceTemplateName("light_brightness")
                .setTargetEntity("light.entrance")
                .putParameterValues("brightness_pct", "80")
                .build()
        )
    }
    
    fun findQuickActionByTrigger(
        trigger: String, 
        quickActions: List<QuickAction>
    ): QuickAction? {
        return quickActions.find { action ->
            trigger.contains(action.voiceTrigger, ignoreCase = true)
        }
    }
}
