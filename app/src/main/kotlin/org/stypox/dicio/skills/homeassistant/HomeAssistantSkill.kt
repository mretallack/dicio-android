package org.stypox.dicio.skills.homeassistant

import kotlinx.coroutines.flow.first
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.HomeAssistant
import org.stypox.dicio.skills.homeassistant.HomeAssistantInfo.homeAssistantDataStore
import java.io.FileNotFoundException

class HomeAssistantSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<HomeAssistant>
) : StandardRecognizerSkill<HomeAssistant>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: HomeAssistant): SkillOutput {
        android.util.Log.d("HomeAssistantSkill", "generateOutput called with inputData: $inputData")
        val settings = ctx.android.homeAssistantDataStore.data.first()
        
        return try {
            when (inputData) {
                is HomeAssistant.GetHelp -> {
                    handleGetHelp()
                }
                is HomeAssistant.GetStatus -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleGetStatus(settings, mapping)
                }
                is HomeAssistant.GetPersonLocation -> {
                    val personName = inputData.personName?.trim() ?: ""
                    val mapping = findBestMatch(personName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(personName)
                    handleGetStatus(settings, mapping)
                }
                is HomeAssistant.SetStateOn -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSetState(settings, mapping, "on")
                }
                is HomeAssistant.SetStateOff -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSetState(settings, mapping, "off")
                }
                is HomeAssistant.SetStateToggle -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSetState(settings, mapping, "toggle")
                }
                is HomeAssistant.SelectSource -> {
                    val entityName = inputData.entityName ?: ""
                    val sourceName = inputData.sourceName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSelectSource(settings, mapping, sourceName)
                }
            }
        } catch (e: FileNotFoundException) {
            HomeAssistantOutput.EntityNotFound("unknown")
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                HomeAssistantOutput.AuthFailed()
            } else {
                HomeAssistantOutput.ConnectionFailed()
            }
        }
    }

    private fun handleGetHelp(): SkillOutput {
        return HomeAssistantOutput.HelpResponse()
    }

    private suspend fun handleGetStatus(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping
    ): SkillOutput {
        val state = HomeAssistantApi.getEntityState(
            settings.baseUrl,
            settings.accessToken,
            mapping.entityId
        )
        
        return HomeAssistantOutput.GetStatusSuccess(
            entityId = mapping.entityId,
            friendlyName = mapping.friendlyName,
            state = state.getString("state"),
            attributes = state.optJSONObject("attributes")
        )
    }

    private suspend fun handleSetState(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping,
        action: String
    ): SkillOutput {
        android.util.Log.d("HomeAssistantSkill", "handleSetState - action: '$action', entityId: '${mapping.entityId}'")
        val domain = mapping.entityId.substringBefore(".")
        android.util.Log.d("HomeAssistantSkill", "Domain: '$domain'")
        val parsedAction = parseAction(action, domain)
        if (parsedAction == null) {
            android.util.Log.e("HomeAssistantSkill", "Failed to parse action: '$action'")
            return HomeAssistantOutput.InvalidAction(action.ifEmpty { "<empty>" }, domain)
        }
        android.util.Log.d("HomeAssistantSkill", "Parsed action: service='${parsedAction.service}', spokenForm='${parsedAction.spokenForm}'")
        
        val service = when (domain) {
            "cover" -> when (parsedAction.service) {
                "turn_on" -> "open_cover"
                "turn_off" -> "close_cover"
                else -> parsedAction.service
            }
            "lock" -> when (parsedAction.service) {
                "turn_on" -> "unlock"
                "turn_off" -> "lock"
                else -> parsedAction.service
            }
            else -> parsedAction.service
        }
        
        HomeAssistantApi.callService(
            settings.baseUrl,
            settings.accessToken,
            domain,
            service,
            mapping.entityId
        )
        
        return HomeAssistantOutput.SetStateSuccess(
            entityId = mapping.entityId,
            friendlyName = mapping.friendlyName,
            action = parsedAction.spokenForm
        )
    }

    private suspend fun handleSelectSource(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping,
        requestedSource: String
    ): SkillOutput {
        // Get entity state to retrieve source_list
        val state = HomeAssistantApi.getEntityState(
            settings.baseUrl,
            settings.accessToken,
            mapping.entityId
        )
        
        // Extract source_list attribute
        val attributes = state.optJSONObject("attributes")
        val sourceListJson = attributes?.optJSONArray("source_list")
        
        if (sourceListJson == null || sourceListJson.length() == 0) {
            return HomeAssistantOutput.NoSourceList(mapping.friendlyName)
        }
        
        // Convert to list
        val sourceList = (0 until sourceListJson.length())
            .map { sourceListJson.getString(it) }
        
        // Fuzzy match requested source
        val matchedSource = findBestSourceMatch(requestedSource, sourceList)
            ?: return HomeAssistantOutput.SourceNotFound(
                requestedSource,
                mapping.friendlyName
            )
        
        // Call select_source service
        HomeAssistantApi.callService(
            settings.baseUrl,
            settings.accessToken,
            "media_player",
            "select_source",
            mapping.entityId,
            mapOf("source" to matchedSource)
        )
        
        return HomeAssistantOutput.SelectSourceSuccess(
            entityId = mapping.entityId,
            friendlyName = mapping.friendlyName,
            sourceName = matchedSource
        )
    }

    private fun findBestSourceMatch(requested: String, available: List<String>): String? {
        val normalized = requested.lowercase().trim()
        
        // 1. Exact match (case insensitive)
        available.firstOrNull { it.lowercase() == normalized }?.let { return it }
        
        // 2. Contains match (either direction)
        available.firstOrNull {
            it.lowercase().contains(normalized) ||
            normalized.contains(it.lowercase())
        }?.let { return it }
        
        // 3. Word-based similarity with tie-breaking
        val scored = available.mapIndexed { index, source ->
            Triple(source, calculateSimilarity(normalized, source.lowercase()), index)
        }.filter { it.second >= 0.5 }
        
        // When multiple sources have same score:
        // 1. Prefer higher similarity
        // 2. Then prefer shorter match
        // 3. Then prefer earlier in list (original order)
        return scored.maxWithOrNull(
            compareBy({ it.second }, { -it.first.length }, { -it.third })
        )?.first
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.split(Regex("\\s+")).toSet()
        val words2 = s2.split(Regex("\\s+")).toSet()
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return if (union > 0) intersection.toDouble() / union else 0.0
    }

    private fun findBestMatch(spokenName: String, mappings: List<EntityMapping>): EntityMapping? {
        val normalized = spokenName.lowercase().replace(Regex("\\b(the|a|an)\\b"), "").trim()
        
        mappings.firstOrNull { it.friendlyName.lowercase() == normalized }?.let { return it }
        
        return mappings.firstOrNull {
            it.friendlyName.lowercase().contains(normalized) ||
            normalized.contains(it.friendlyName.lowercase())
        }
    }

    private data class ParsedAction(val service: String, val spokenForm: String)

    private fun parseAction(action: String, domain: String): ParsedAction? {
        val normalized = action.lowercase().trim()
        android.util.Log.d("HomeAssistantSkill", "parseAction - input: '$action', normalized: '$normalized'")
        
        return when {
            normalized.contains("on") || normalized in listOf("open", "unlock", "enable") ->
                ParsedAction("turn_on", "on")
            normalized.contains("off") || normalized in listOf("close", "lock", "disable") ->
                ParsedAction("turn_off", "off")
            normalized.contains("toggle") ->
                ParsedAction("toggle", "toggled")
            else -> null
        }
    }
}
