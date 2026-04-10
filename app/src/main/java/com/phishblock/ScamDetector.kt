package com.phishblock

class ScamDetector {
    private val scamPhrases = listOf(
        "bank", "account", "blocked", "verify", "urgent", "immediately",
        "click here", "update your", "login", "password", "otp", "sms",
        "won", "prize", "lottery", "suspended", "fraud", "security alert",
        "deactivated", "verify now", "confirm your", "alert", "action required"
    )

    fun analyze(text: String): Pair<Int, String> {
        val lowerText = text.lowercase()
        var score = 0
        val reasons = mutableListOf<String>()

        for (phrase in scamPhrases) {
            if (lowerText.contains(phrase)) {
                score += 10
                reasons.add("'$phrase'")
            }
        }

        // Heuristics
        if (lowerText.contains("http")) {
            score += 15
            reasons.add("contains link")
        }
        if (lowerText.contains("!!") || lowerText.contains("!!!")) {
            score += 5
            reasons.add("urgent punctuation")
        }
        if (lowerText.matches(Regex(".*\\d{6,}.*"))) {
            score += 5
            reasons.add("suspicious number")
        }

        val finalScore = score.coerceAtMost(100)
        val reasonText = if (reasons.isNotEmpty()) {
            "Found: ${reasons.take(3).joinToString(", ")}"
        } else {
            "No scam indicators"
        }
        return Pair(finalScore, reasonText)
    }
}