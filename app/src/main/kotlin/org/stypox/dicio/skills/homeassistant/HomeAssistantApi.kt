package org.stypox.dicio.skills.homeassistant

import org.json.JSONArray
import org.json.JSONObject
import org.stypox.dicio.util.ConnectionUtils
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HomeAssistantApi {
    @Throws(IOException::class)
    suspend fun getAllStates(baseUrl: String, token: String): JSONArray {
        val connection = URL("$baseUrl/api/states").openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONArray(response)
    }

    @Throws(IOException::class)
    suspend fun getEntityState(baseUrl: String, token: String, entityId: String): JSONObject {
        val connection = URL("$baseUrl/api/states/$entityId").openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONObject(response)
    }

    @Throws(IOException::class)
    suspend fun callService(
        baseUrl: String,
        token: String,
        domain: String,
        service: String,
        entityId: String
    ): JSONArray {
        val connection = URL("$baseUrl/api/services/$domain/$service").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val body = JSONObject().put("entity_id", entityId).toString()
        connection.outputStream.write(body.toByteArray())
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONArray(response)
    }

    @Throws(IOException::class)
    suspend fun callServiceWithData(
        baseUrl: String,
        token: String,
        domain: String,
        service: String,
        entityId: String,
        data: Map<String, Any>
    ): JSONArray {
        val connection = URL("$baseUrl/api/services/$domain/$service").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val body = JSONObject().apply {
            put("entity_id", entityId)
            data.forEach { (key, value) -> put(key, value) }
        }.toString()
        connection.outputStream.write(body.toByteArray())
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONArray(response)
    }

    @Throws(IOException::class)
    suspend fun callServiceWithTemplate(
        baseUrl: String,
        token: String,
        template: ServiceTemplate,
        entityId: String?,
        parameters: Map<String, String>
    ): JSONArray {
        val connection = URL("$baseUrl/api/services/${template.domain}/${template.service}").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val body = JSONObject().apply {
            if (entityId != null) {
                put("entity_id", entityId)
            }
            
            // Add template parameters with type conversion
            for ((key, value) in parameters) {
                val param = template.parametersList.find { it.key == key }
                when (param?.valueType) {
                    "number" -> put(key, value.toDoubleOrNull() ?: value)
                    "boolean" -> put(key, value.toBooleanStrictOrNull() ?: value)
                    else -> put(key, value)
                }
            }
        }.toString()
        
        connection.outputStream.write(body.toByteArray())
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONArray(response)
    }

    @Throws(IOException::class)
    suspend fun executeQuickAction(
        baseUrl: String,
        token: String,
        quickAction: QuickAction,
        template: ServiceTemplate,
        entityId: String?
    ): JSONArray {
        return callServiceWithTemplate(
            baseUrl, 
            token, 
            template, 
            entityId ?: quickAction.targetEntity,
            quickAction.parameterValuesMap
        )
    }

    @Throws(IOException::class)
    suspend fun testServiceCall(
        baseUrl: String,
        token: String,
        domain: String,
        service: String
    ): Boolean {
        return try {
            val connection = URL("$baseUrl/api/services").openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            
            val scanner = java.util.Scanner(connection.inputStream)
            val response = scanner.useDelimiter("\\A").next()
            scanner.close()
            
            val services = JSONArray(response)
            for (i in 0 until services.length()) {
                val serviceObj = services.getJSONObject(i)
                if (serviceObj.getString("domain") == domain) {
                    val servicesList = serviceObj.getJSONObject("services")
                    return servicesList.has(service)
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
