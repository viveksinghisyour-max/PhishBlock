package com.phishblock

import com.phishblock.models.Message
import kotlinx.coroutines.*

class MessageAnalyzer(
    private val safeBrowsingClient: SafeBrowsingClient,
    private val scamDetector: ScamDetector
) {
    suspend fun analyze(message: Message): Message {
        val urls = safeBrowsingClient.extractUrls(message.text)
        var isMalicious = false
        val reasons = mutableListOf<String>()

        // 1. Check URLs
        for (url in urls) {
            if (safeBrowsingClient.checkUrl(url)) {
                isMalicious = true
                reasons.add("Malicious URL: $url")
                break
            }
        }

        // 2. Check text content
        val (score, scamReason) = scamDetector.analyze(message.text)
        if (score > 50) {
            isMalicious = true
            reasons.add(scamReason)
        }

        message.isMalicious = isMalicious
        message.riskScore = score
        message.reason = reasons.joinToString("; ")
        return message
    }
}