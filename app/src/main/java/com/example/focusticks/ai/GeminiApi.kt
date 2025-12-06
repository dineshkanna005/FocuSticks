package com.example.focusticks.ai

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AiTask(
    val title: String,
    val subject: String,
    val category: String,
    val difficulty: String,
    val due: String
)

object GeminiApi {

    private const val API_KEY = "AIzaSyBOc3dDclBdd9MV9V3uQ71QjAH9WjlyDK0"
    private const val MODEL = "models/gemini-2.0-flash-vision"

    fun extractTasks(bitmap: Bitmap): List<AiTask> {
        return try {
            val base64Image = bitmapToBase64(bitmap)

            val contents = JSONArray()
            val parts = JSONArray()

            val imagePart = JSONObject()
            val imageData = JSONObject()
            imageData.put("mimeType", "image/jpeg")
            imageData.put("data", base64Image)
            imagePart.put("inlineData", imageData)
            parts.put(imagePart)

            val promptPart = JSONObject()
            promptPart.put(
                "text",
                """
                Extract exactly ONE study task from this handwritten image.
                Always return ONLY a JSON array in this format:
                [
                  {
                    "title": "...",
                    "subject": "...",
                    "category": "...",
                    "difficulty": "...",
                    "due": "MM/dd/yyyy HH:mm"
                  }
                ]

                Rules:
                - Read all handwriting.
                - "title": short action, e.g. "Complete math assignment".
                - Convert date phrases like "December 10 at 5PM" to "12/10/2025 17:00".
                - If subject missing, infer from text.
                - If difficulty unclear, choose Easy/Medium/Hard logically.
                - If category missing, guess Homework/Project/Exam.
                - Never return explanation, markdown, or text outside the JSON array.
                """.trimIndent()
            )
            parts.put(promptPart)

            val userContent = JSONObject()
            userContent.put("role", "user")
            userContent.put("parts", parts)
            contents.put(userContent)

            val root = JSONObject()
            root.put("contents", contents)

            val url = URL("https://generativelanguage.googleapis.com/v1beta/$MODEL:generateContent?key=$API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.outputStream.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }

            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
            val responseText = InputStreamReader(stream).readText()
            conn.disconnect()

            parseTasksFromResponse(responseText)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseTasksFromResponse(response: String): List<AiTask> {
        return try {
            val result = JSONObject(response)
            val text = result
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonText = extractJsonArrayString(text)
            val arr = JSONArray(jsonText)

            val out = mutableListOf<AiTask>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    AiTask(
                        title = o.optString("title", "Untitled task"),
                        subject = o.optString("subject", "General"),
                        category = o.optString("category", "Assignment"),
                        difficulty = o.optString("difficulty", "Medium"),
                        due = o.optString("due", "")
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractJsonArrayString(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val last = trimmed.lastIndexOf("]")
            if (last >= 0) return trimmed.substring(0, last + 1)
        }
        val start = trimmed.indexOf('[')
        val end = trimmed.lastIndexOf(']')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        throw JSONException("No JSON array found in: $text")
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
