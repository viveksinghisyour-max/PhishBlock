package com.phishblock

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Helper class to interact with Google Safe Browsing API v4.
 */
class SafeBrowsingHelper(private val apiKey: String) {

    private val service: SafeBrowsingService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://safebrowsing.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        service = retrofit.create(SafeBrowsingService::class.java)
    }

    /**
     * Checks if a URL is safe using Google Safe Browsing API.
     * @return true if the URL is classified as unsafe (matches found), false otherwise.
     */
    suspend fun isUrlUnsafe(url: String): Boolean {
        if (apiKey.isEmpty()) return false

        val request = SafeBrowsingRequest(
            client = ClientInfo(),
            threatInfo = ThreatInfo(
                threatEntries = listOf(ThreatEntry(url = url))
            )
        )

        return try {
            val response = service.findThreatMatches(apiKey, request)
            !response.matches.isNullOrEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    interface SafeBrowsingService {
        @POST("v4/threatMatches:find")
        suspend fun findThreatMatches(
            @Query("key") apiKey: String,
            @Body request: SafeBrowsingRequest
        ): SafeBrowsingResponse
    }

    // --- Request Data Classes ---

    data class SafeBrowsingRequest(
        val client: ClientInfo,
        val threatInfo: ThreatInfo
    )

    data class ClientInfo(
        val clientId: String = "phishblock",
        val clientVersion: String = "1.0.0"
    )

    data class ThreatInfo(
        val threatTypes: List<String> = listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"),
        val platformTypes: List<String> = listOf("ANY_PLATFORM"),
        val threatEntryTypes: List<String> = listOf("URL"),
        val threatEntries: List<ThreatEntry>
    )

    data class ThreatEntry(
        val url: String
    )

    // --- Response Data Classes ---

    data class SafeBrowsingResponse(
        val matches: List<ThreatMatch>? = null
    )

    data class ThreatMatch(
        val threatType: String,
        val platformType: String,
        val threatEntryType: String,
        val threat: ThreatEntry
    )
}
