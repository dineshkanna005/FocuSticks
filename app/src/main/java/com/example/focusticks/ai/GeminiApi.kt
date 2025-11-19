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
        val content = JSONArray()
        val parts = JSONArray()
        val imageObj = JSONObject()
        val inlineData = JSONObject()

        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Image)
        imageObj.put("inlineData", inlineData)
        parts.put(imageObj)

        val textObj = JSONObject()
        textObj.put("text", "Extract tasks with fields: title, subject, category, difficulty, due (MM/dd/yyyy HH:mm). Reply only JSON array.")
        parts.put(textObj)

        val item = JSONObject()
        item.put("role", "user")
        item.put("parts", parts)
        content.put(item)

        json.put("contents", content)

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
                    o.getString("title"),
                    o.getString("subject"),
                    o.getString("category"),
                    o.getString("difficulty"),
                    o.getString("due")
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
