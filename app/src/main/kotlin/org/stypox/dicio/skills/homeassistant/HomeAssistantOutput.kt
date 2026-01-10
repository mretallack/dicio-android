package org.stypox.dicio.skills.homeassistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.json.JSONObject
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString

sealed interface HomeAssistantOutput : SkillOutput {
    data class GetStatusSuccess(
        val entityId: String,
        val friendlyName: String,
        val state: String,
        val attributes: JSONObject?
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_entity_state,
            friendlyName,
            state
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_entity_state, "", state),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class SetStateSuccess(
        val entityId: String,
        val friendlyName: String,
        val action: String
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_set_success,
            friendlyName,
            action
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_set_success, "", action),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class EntityNotMapped(
        val entityName: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_entity_not_mapped,
            entityName
        )
    }

    data class EntityNotFound(
        val entityId: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_entity_not_found,
            entityId
        )
    }

    data class InvalidAction(
        val action: String,
        val entityType: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_invalid_action,
            action,
            entityType
        )
    }

    class ConnectionFailed : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_connection_failed
        )
    }

    class AuthFailed : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_auth_failed
        )
    }

    data class CallServiceSuccess(
        val entityId: String,
        val friendlyName: String,
        val service: String,
        val command: String
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_service_success,
            command,
            friendlyName
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_service_success, command, ""),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class ServiceTemplateSuccess(
        val templateName: String,
        val entityId: String,
        val parameters: Map<String, String>
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = 
            "Successfully executed $templateName on $entityId"

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = templateName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Parameters: ${parameters.entries.joinToString { "${it.key}: ${it.value}" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class ServiceTemplateFailed(
        val templateName: String,
        val error: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = 
            "Failed to execute $templateName: $error"
    }

    data class QuickActionSuccess(
        val actionName: String,
        val message: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = message
    }

    data class QuickActionFailed(
        val actionName: String,
        val error: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = 
            "Failed to execute $actionName: $error"
    }
}
