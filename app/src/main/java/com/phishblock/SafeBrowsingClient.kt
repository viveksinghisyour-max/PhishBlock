package com.phishblock

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.regex.Pattern

class SafeBrowsingClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val urlRegex = Pattern.compile("(https?://[\\w\\d\\-._~:/?#\\[\\]@!$&'()*+,;=]+)")

    fun extractUrls(text: String): List<String> {
        val matcher = urlRegex.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }

    suspend fun checkUrl(url: String): Boolean {
        if (apiKey.isEmpty()) return false

        val requestBody = """
            {
                "client": {
                    "clientId": "phishblock",
                    "clientVersion": "1.0.0"
                },
                "threatInfo": {
                    "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"],
                    "platformTypes": ["ANY_PLATFORM"],
                    "threatEntryTypes": ["URL"],
                    "threatEntries": [{"url": "$url"}]
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                body?.contains("matches") == true
            }
        } catch (e: Exception) {
            false
        }
    }
}