package org.stypox.dicio.skills.homeassistant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles import/export of Home Assistant configuration (entity mappings, URL, token)
 * using Kotlin serialization with JSON format.
 */
object HomeAssistantYamlUtils {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Serializable
    data class YamlEntityMapping(
        @SerialName("friendly_name") val friendlyName: String,
        @SerialName("entity_id") val entityId: String
    )

    @Serializable
    data class YamlHomeAssistantConfig(
        @SerialName("base_url") val baseUrl: String = "",
        @SerialName("access_token") val accessToken: String = "",
        @SerialName("entity_mappings") val entityMappings: List<YamlEntityMapping> = emptyList()
    )

    fun exportToYaml(
        baseUrl: String,
        accessToken: String,
        mappings: List<EntityMapping>,
        outputStream: OutputStream
    ) {
        val config = YamlHomeAssistantConfig(
            baseUrl = baseUrl,
            accessToken = accessToken,
            entityMappings = mappings.map {
                YamlEntityMapping(it.friendlyName, it.entityId)
            }
        )
        outputStream.writer().use { it.write(json.encodeToString(config)) }
    }

    fun importFromYaml(inputStream: InputStream): YamlHomeAssistantConfig {
        val text = inputStream.reader().use { it.readText() }
        return json.decodeFromString(text)
    }
}
