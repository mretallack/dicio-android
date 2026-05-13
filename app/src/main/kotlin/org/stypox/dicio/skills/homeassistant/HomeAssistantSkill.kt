package org.stypox.dicio.skills.homeassistant

import kotlinx.coroutines.flow.first
import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.json.JSONArray
import org.json.JSONObject
import org.stypox.dicio.sentences.Sentences.HomeAssistant
import org.stypox.dicio.skills.homeassistant.HomeAssistantInfo.homeAssistantDataStore
import org.stypox.dicio.util.StringUtils
import java.io.FileNotFoundException

class HomeAssistantSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<HomeAssistant>
) : StandardRecognizerSkill<HomeAssistant>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: HomeAssistant): SkillOutput {
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
                    // Parse the action at the call site, so handleSetState receives
                    // a validated ParsedAction rather than a raw string.
                    val domain = mapping.entityId.substringBefore(".")
                    val action = parseAction("on", domain)
                        ?: return HomeAssistantOutput.InvalidAction("on", domain)
                    handleSetState(settings, mapping, action)
                }
                is HomeAssistant.SetStateOff -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    val domain = mapping.entityId.substringBefore(".")
                    val action = parseAction("off", domain)
                        ?: return HomeAssistantOutput.InvalidAction("off", domain)
                    handleSetState(settings, mapping, action)
                }
                is HomeAssistant.SetStateToggle -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    val domain = mapping.entityId.substringBefore(".")
                    val action = parseAction("toggle", domain)
                        ?: return HomeAssistantOutput.InvalidAction("toggle", domain)
                    handleSetState(settings, mapping, action)
                }
                is HomeAssistant.SelectSource -> {
                    val entityName = inputData.entityName ?: ""
                    val sourceName = inputData.sourceName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSelectSource(ctx, settings, mapping, sourceName)
                }
                is HomeAssistant.VacuumRoom -> {
                    val roomNames = mutableListOf<String>()
                    inputData.roomNames?.trim()?.let { if (it.isNotEmpty()) roomNames.add(it) }
                    inputData.roomNames2?.trim()?.let { if (it.isNotEmpty()) roomNames.add(it) }
                    if (roomNames.isEmpty()) return HomeAssistantOutput.VacuumRoomNotFound("", emptyList())
                    handleVacuumRoom(settings, roomNames)
                }
                is HomeAssistant.VacuumStart -> handleVacuumCommand(settings, "start")
                is HomeAssistant.VacuumStop -> handleVacuumCommand(settings, "stop")
                is HomeAssistant.VacuumPause -> handleVacuumCommand(settings, "pause")
                is HomeAssistant.VacuumDock -> handleVacuumCommand(settings, "return_to_base")
            }
        // Only catch exceptions we can meaningfully handle; let unrecognized
        // exceptions propagate so Dicio's infrastructure shows the error to the user.
        } catch (e: FileNotFoundException) {
            HomeAssistantOutput.EntityNotFound("unknown")
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                HomeAssistantOutput.AuthFailed()
            } else {
                throw e
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

    /**
     * Handles state changes (on/off/toggle) for an entity.
     * Accepts a pre-validated [ParsedAction] so that action parsing happens at the call site,
     * keeping this method focused on the HA API call and domain-specific service mapping.
     */
    private suspend fun handleSetState(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping,
        parsedAction: ParsedAction
    ): SkillOutput {
        val domain = mapping.entityId.substringBefore(".")

        // Map generic on/off services to domain-specific equivalents
        // (e.g. cover uses open_cover/close_cover, lock uses lock/unlock)
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
        ctx: SkillContext,
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

        // Use dicio-numbers to convert spoken number words to digits (e.g. "two" -> "2").
        // Note: homophone variations (e.g. "too" -> "2") are not yet supported by dicio-numbers.
        val normalizedSource = normalizeNumberWords(ctx, requestedSource)

        // Fuzzy match using StringUtils.customStringDistance (Levenshtein-based)
        val matchedSource = findBestSourceMatch(normalizedSource, sourceList)
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

    /**
     * Uses dicio-numbers [ParserFormatter] to convert spoken number words to their digit form.
     * For example, "BBC Radio two" becomes "BBC Radio 2".
     * Falls back to the original input if no parser is available for the current locale.
     */
    private fun normalizeNumberWords(ctx: SkillContext, input: String): String {
        val pf = ctx.parserFormatter ?: return input
        val parts = pf.extractNumber(input).parseMixedWithText()
        return parts.joinToString("") { part ->
            when (part) {
                is Number -> part.integerValue().toString()
                else -> part.toString()
            }
        }.trim()
    }

    /**
     * Finds the best matching source from [available] for the [requested] source name,
     * using [StringUtils.customStringDistance] (Levenshtein-based with subsequence bonuses).
     * Returns null if no source is close enough.
     */
    private fun findBestSourceMatch(requested: String, available: List<String>): String? {
        // Try exact match first (case-insensitive)
        val normalized = requested.lowercase().trim()
        available.firstOrNull { it.lowercase() == normalized }?.let { return it }

        // Use StringUtils.customStringDistance — lower is better.
        // The threshold scales with input length but is capped to avoid false positives
        // on short inputs matching long source names.
        val maxAcceptableDistance = (normalized.length / 3).coerceAtLeast(2)
        return available
            .map { it to StringUtils.customStringDistance(normalized, it) }
            .filter { it.second <= maxAcceptableDistance }
            .minByOrNull { it.second }
            ?.first
    }

    private fun findVacuumEntity(mappings: List<EntityMapping>): EntityMapping? {
        return mappings.firstOrNull { it.entityId.startsWith("vacuum.") }
    }

    private suspend fun handleVacuumRoom(
        settings: SkillSettingsHomeAssistant,
        spokenRoomNames: List<String>
    ): SkillOutput {
        val vacuumMapping = findVacuumEntity(settings.entityMappingsList)
            ?: return HomeAssistantOutput.VacuumNotConfigured()

        val state = HomeAssistantApi.getEntityState(
            settings.baseUrl, settings.accessToken, vacuumMapping.entityId
        )
        val rooms = parseRooms(state.optJSONObject("attributes"))
        if (rooms.isEmpty()) return HomeAssistantOutput.VacuumRoomNotFound("", emptyList())

        val matchedRooms = mutableListOf<Pair<Int, String>>()
        for (spoken in spokenRoomNames) {
            val matched = matchRoomName(spoken, rooms)
            if (matched != null) {
                matchedRooms.add(matched.id to matched.name)
            } else {
                return HomeAssistantOutput.VacuumRoomNotFound(spoken, rooms.map { it.name })
            }
        }

        val segmentIds = matchedRooms.map { it.first }
        val body = JSONObject()
            .put("entity_id", vacuumMapping.entityId)
            .put("segments", JSONArray(segmentIds))

        HomeAssistantApi.callServiceWithBody(
            settings.baseUrl, settings.accessToken,
            "dreame_vacuum", "vacuum_clean_segment", body
        )

        return HomeAssistantOutput.VacuumRoomSuccess(matchedRooms.map { it.second })
    }

    private suspend fun handleVacuumCommand(
        settings: SkillSettingsHomeAssistant,
        service: String
    ): SkillOutput {
        val vacuumMapping = findVacuumEntity(settings.entityMappingsList)
            ?: return HomeAssistantOutput.VacuumNotConfigured()

        HomeAssistantApi.callService(
            settings.baseUrl, settings.accessToken,
            "vacuum", service, vacuumMapping.entityId
        )

        val action = when (service) {
            "start" -> "started"
            "stop" -> "stopped"
            "pause" -> "paused"
            "return_to_base" -> "sent home"
            else -> service
        }
        return HomeAssistantOutput.VacuumCommandSuccess(action)
    }

    internal data class RoomInfo(val id: Int, val name: String)

    private fun parseRooms(attributes: JSONObject?): List<RoomInfo> {
        val roomsObj = attributes?.optJSONObject("rooms") ?: return emptyList()
        val result = mutableListOf<RoomInfo>()
        for (mapName in roomsObj.keys()) {
            val roomArray = roomsObj.optJSONArray(mapName) ?: continue
            for (i in 0 until roomArray.length()) {
                val room = roomArray.optJSONObject(i) ?: continue
                result.add(RoomInfo(room.getInt("id"), room.getString("name")))
            }
        }
        return result
    }

    internal fun matchRoomName(spoken: String, rooms: List<RoomInfo>): RoomInfo? {
        val normalized = spoken.lowercase().trim()
        // Exact match
        rooms.firstOrNull { it.name.lowercase() == normalized }?.let { return it }
        // Contains match
        rooms.firstOrNull { it.name.lowercase().contains(normalized) || normalized.contains(it.name.lowercase()) }?.let { return it }
        // Fuzzy match
        val maxDistance = (normalized.length / 3).coerceAtLeast(2)
        return rooms
            .map { it to StringUtils.customStringDistance(normalized, it.name.lowercase()) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
            ?.first
    }

    private fun findBestMatch(spokenName: String, mappings: List<EntityMapping>): EntityMapping? {
        val normalized = spokenName.lowercase().replace(Regex("\\b(the|a|an)\\b"), "").trim()
        
        mappings.firstOrNull { it.friendlyName.lowercase() == normalized }?.let { return it }
        
        return mappings.firstOrNull {
            it.friendlyName.lowercase().contains(normalized) ||
            normalized.contains(it.friendlyName.lowercase())
        }
    }

    /**
     * Represents a validated action parsed from user input.
     * @property service the HA service name (e.g. "turn_on", "turn_off", "toggle")
     * @property spokenForm the human-readable form shown in output (e.g. "on", "off", "toggled")
     */
    private data class ParsedAction(val service: String, val spokenForm: String)

    private fun parseAction(action: String, domain: String): ParsedAction? {
        val normalized = action.lowercase().trim()
        
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
