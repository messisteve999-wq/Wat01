package com.steve.wat01

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    // Hypothèse : le serveur accepte POST {server}/chat avec {"system","prompt"} et répond {"reply": "..."}
    fun ask(c: Context, prompt: String): String {
        val url = URL(Prefs.serverUrl(c).trimEnd('/') + "/chat")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15000
            readTimeout = 30000
        }
        val body = JSONObject().put("system", Prefs.persona(c)).put("prompt", prompt).toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        val text = conn.inputStream.bufferedReader().readText()
        return JSONObject(text).optString("reply", text)
    }

    fun suggestions(c: Context, message: String): List<String> {
        val raw = ask(c, "Propose 3 réponses courtes à ce message, une par ligne, sans numérotation : $message")
        return raw.lines().map { it.trim().trimStart('-', '•', ' ') }.filter { it.isNotEmpty() }.take(3)
    }
}
