package com.example.focusticks.ai

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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

    private const val API_KEY = "YOUR_API_KEY"
    private const val MODEL = "models/gemini-2.0-flash-vision"

    fun extractTasks(bitmap: Bitmap): List<AiTask> {

        val base64Image = bitmapToBase64(bitmap)

        val json = JSONObject()
        val contents = JSONArray()
        val parts = JSONArray()

        val imagePart = JSONObject()
        val imageData = JSONObject()

        imageData.put("mimeType", "image/jpeg")
        imageData.put("data", base64Image)
        imagePart.put("inlineData", imageData)
        parts.put(imagePart)

        val prompt = JSONObject()
        prompt.put(
            "text",
            "You are a task extraction system. Extract tasks from this image. " +
                    "Return ONLY a JSON array where each item has: " +
                    "title, subject, category, difficulty, due (MM/dd/yyyy HH:mm). " +
                    "If details missing, guess reasonably. No extra text."
        )
        parts.put(prompt)

        val item = JSONObject()
        item.put("role", "user")
        item.put("parts", parts)
        contents.put(item)

        json.put("contents", contents)

        val url = URL("https://generativelanguage.googleapis.com/v1beta/$MODEL:generateContent?key=$API_KEY")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use { it.write(json.toString().toByteArray()) }

        val response = conn.inputStream.bufferedReader().readText()
        val result = JSONObject(response)

        val text = result
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val arr = JSONArray(text)
        val out = mutableListOf<AiTask>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                AiTask(
                    o.optString("title", ""),
                    o.optString("subject", "General"),
                    o.optString("category", "Assignment"),
                    o.optString("difficulty", "Medium"),
                    o.optString("due", "")
                )
            )
        }

        return out
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
