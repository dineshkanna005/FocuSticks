package com.example.focusticks.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class AiTaskEditable(
    val title: String,
    val subject: String,
    val difficulty: String,
    val category: String,
    val due: String,
    val reminder: String
)

object OpenAIApi {

    private const val MODEL = "gpt-4o-mini"

    // IMPORTANT: No newline, no trailing spaces
    private const val RAW_KEY =
        "OPENAI_API_KEY"

    private val API_KEY = RAW_KEY.trim()

    suspend fun extractTasks(bmp: Bitmap): List<AiTaskEditable> =
        withContext(Dispatchers.IO) {

            val base64 = bitmapToBase64(bmp)

            val prompt = """
                Extract ONE task from this handwriting image.
                Return ONLY JSON in this format:
                {
                  "title": "",
                  "subject": "",
                  "difficulty": "",
                  "category": "",
                  "due": "MM/dd/yyyy HH:mm",
                  "reminder": ""
                }
                Difficulty must be Easy, Medium or Hard.
                Use 2025 if year missing.
                Reminder must be numbers only.
            """.trimIndent()

            // build content array
            val contentJson = JSONArray()
                .put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                .put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64")
                    })
                })

            val messages = JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", contentJson)
                }
            )

            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", messages)
                put("response_format", JSONObject().put("type", "json_object"))
            }

            val requestBody = RequestBody.create(
                "application/json".toMediaType(),
                body.toString()
            )

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")   // CLEAN, NO NEWLINE
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            val responseText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                println("🔥 OPENAI ERROR: $responseText")
                return@withContext emptyList()
            }

            return@withContext try {
                val json = JSONObject(responseText)
                val content = json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val parsed = JSONObject(content)

                listOf(
                    AiTaskEditable(
                        parsed.optString("title"),
                        parsed.optString("subject"),
                        parsed.optString("difficulty"),
                        parsed.optString("category"),
                        parsed.optString("due"),
                        parsed.optString("reminder")
                    )
                )
            } catch (e: Exception) {
                println("🔥 PARSE ERROR: ${e.message}")
                emptyList()
            }
        }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
