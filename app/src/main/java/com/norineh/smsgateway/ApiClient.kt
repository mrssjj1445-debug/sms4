package com.norineh.smsgateway

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SmsJob(
    val id: String,
    val phone: String,
    val message: String
)

class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(
            Config.HTTP_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        .readTimeout(
            Config.HTTP_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        .writeTimeout(
            Config.HTTP_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        .build()

    fun getPending(): List<SmsJob> {

        val request = Request.Builder()
            .url(Config.PENDING_URL)
            .get()
            .header("Accept", "application/json")
            .header(
                "Authorization",
                "Bearer ${Config.GATEWAY_TOKEN}"
            )
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }

            return parseJobs(
                response.body?.string().orEmpty()
            )
        }
    }

    fun reportResult(
        id: String,
        success: Boolean,
        error: String? = null
    ) {

        val json = JSONObject()
            .put("id", id)
            .put(
                "status",
                if (success) "sent" else "failed"
            )
            .put("success", success)

        if (!error.isNullOrBlank()) {
            json.put("error", error)
        }

        val body = json.toString()
            .toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )

        val request = Request.Builder()
            .url(Config.RESULT_URL)
            .post(body)
            .header("Accept", "application/json")
            .header(
                "Authorization",
                "Bearer ${Config.GATEWAY_TOKEN}"
            )
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                error("Result HTTP ${response.code}")
            }
        }
    }

    private fun parseJobs(
        raw: String
    ): List<SmsJob> {

        if (raw.isBlank()) {
            return emptyList()
        }

        val root = JSONObject(raw)

        if (
            root.has("success") &&
            !root.optBoolean("success", true)
        ) {
            return emptyList()
        }

        val value = when {

            root.has("data") ->
                root.get("data")

            root.has("jobs") ->
                root.get("jobs")

            root.has("messages") ->
                root.get("messages")

            else ->
                root
        }

        val array = when (value) {

            is JSONArray ->
                value

            is JSONObject ->
                JSONArray().put(value)

            else ->
                JSONArray()
        }

        val result = mutableListOf<SmsJob>()

        for (i in 0 until array.length()) {

            val obj =
                array.optJSONObject(i)
                    ?: continue

            val id =
                obj.optString("id")
                    .ifBlank {
                        obj.optString("sms_id")
                    }
                    .trim()

            val phone =
                obj.optString("phone")
                    .ifBlank {
                        obj.optString("mobile")
                            .ifBlank {
                                obj.optString("to")
                            }
                    }
                    .trim()

            val message =
                obj.optString("message")
                    .ifBlank {
                        obj.optString("text")
                            .ifBlank {
                                obj.optString("body")
                            }
                    }
                    .trim()

            if (
                id.isNotBlank() &&
                phone.isNotBlank() &&
                message.isNotBlank()
            ) {
                result += SmsJob(
                    id,
                    phone,
                    message
                )
            }
        }

        return result
    }
}
