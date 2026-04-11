package com.phishblock.models

import android.net.Uri
import java.util.regex.Pattern
import kotlin.math.log2

/**
 * Extracts 62 numerical features from a URL for phishing detection.
 * The features and their order must match the training dataset exactly.
 */
class FeatureExtractor {

    /**
     * Extracts features from the given URL and returns them as a FloatArray of size 62.
     */
    fun extractFeatures(url: String): FloatArray {
        val features = FloatArray(62)
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            null
        }

        val hostname = uri?.host ?: ""
        val path = uri?.path ?: ""
        val query = uri?.query ?: ""
        val fragment = uri?.fragment ?: ""

        // 1. LengthOfURL
        features[0] = url.length.toFloat()

        // 2. URLComplexity
        val specialCharsCount = url.count { !it.isLetterOrDigit() && it !in "/:." }
        features[1] = if (url.isNotEmpty()) specialCharsCount.toFloat() / url.length else 0f

        // 3. CharacterComplexity
        val uniqueChars = url.toSet().size
        features[2] = if (url.isNotEmpty()) uniqueChars.toFloat() / url.length else 0f

        // 4. DomainLengthOfURL
        features[3] = hostname.length.toFloat()

        // 5. IsDomainIP
        features[4] = if (isIpAddress(hostname)) 1f else 0f

        // 6. TLDLength
        features[5] = hostname.substringAfterLast('.', "").length.toFloat()

        // 7. LetterCntInURL
        val letterCount = url.count { it.isLetter() }
        features[6] = letterCount.toFloat()

        // 8. URLLetterRatio
        features[7] = if (url.isNotEmpty()) letterCount.toFloat() / url.length else 0f

        // 9. DigitCntInURL
        val digitCount = url.count { it.isDigit() }
        features[8] = digitCount.toFloat()

        // 10. URLDigitRatio
        features[9] = if (url.isNotEmpty()) digitCount.toFloat() / url.length else 0f

        // 11. EqualCharCntInURL
        features[10] = url.count { it == '=' }.toFloat()

        // 12. QuesMarkCntInURL
        features[11] = url.count { it == '?' }.toFloat()

        // 13. AmpCharCntInURL
        features[12] = url.count { it == '&' }.toFloat()

        // 14. OtherSpclCharCntInURL
        features[13] = url.count { !it.isLetterOrDigit() && it !in "=?&" }.toFloat()

        // 15. URLOtherSpclCharRatio
        features[14] = if (url.isNotEmpty()) features[13] / url.length else 0f

        // 16. NumberOfHashtags
        features[15] = url.count { it == '#' }.toFloat()

        // 17. NumberOfSubdomains
        features[16] = hostname.count { it == '.' }.toFloat()

        // 18. HavingPath
        features[17] = if (path.isNotEmpty() && path != "/") 1f else 0f

        // 19. PathLength
        features[18] = path.length.toFloat()

        // 20. HavingQuery
        features[19] = if (query.isNotEmpty()) 1f else 0f

        // 21. HavingFragment
        features[20] = if (fragment.isNotEmpty()) 1f else 0f

        // 22. HavingAnchor
        features[21] = if (url.contains("#")) 1f else 0f

        // 23. HasSSL
        features[22] = if (uri?.scheme == "https") 1f else 0f

        // 24. IsUnreachable (Requires network, setting 0)
        features[23] = 0f

        // 25. LineOfCode (Requires HTML, setting 0)
        features[24] = 0f

        // 26. LongestLineLength (Requires HTML, setting 0)
        features[25] = 0f

        // 27. HasTitle (Requires HTML, setting 0)
        features[26] = 0f

        // 28. HasFavicon (Requires HTML, setting 0)
        features[27] = 0f

        // 29. HasRobotsBlocked (Requires robots.txt, setting 0)
        features[28] = 0f

        // 30. IsResponsive (Requires HTML/CSS, setting 0)
        features[29] = 0f

        // 31. IsURLRedirects (Requires network, setting 0)
        features[30] = 0f

        // 32. IsSelfRedirects (Requires HTML, setting 0)
        features[31] = 0f

        // 33. HasDescription (Requires HTML, setting 0)
        features[32] = 0f

        // 34. HasPopup (Requires HTML, setting 0)
        features[33] = 0f

        // 35. HasIFrame (Requires HTML, setting 0)
        features[34] = 0f

        // 36. IsFormSubmitExternal (Requires HTML, setting 0)
        features[35] = 0f

        // 37. HasSocialMediaPage (Requires HTML, setting 0)
        features[36] = 0f

        // 38. HasSubmitButton (Requires HTML, setting 0)
        features[37] = 0f

        // 39. HasHiddenFields (Requires HTML, setting 0)
        features[38] = 0f

        // 40. HasPasswordFields (Requires HTML, setting 0)
        features[39] = 0f

        // 41. HasBankingKey
        features[40] = if (containsKeywords(url, listOf("bank", "login", "signin", "account", "secure"))) 1f else 0f

        // 42. HasPaymentKey
        features[41] = if (containsKeywords(url, listOf("pay", "checkout", "paypal", "card", "money", "transfer"))) 1f else 0f

        // 43. HasCryptoKey
        features[42] = if (containsKeywords(url, listOf("crypto", "wallet", "btc", "eth", "binance", "coinbase"))) 1f else 0f

        // 44. HasCopyrightInfoKey
        features[43] = if (url.contains("copyright", ignoreCase = true)) 1f else 0f

        // 45. CntImages (Requires HTML, setting 0)
        features[44] = 0f

        // 46. CntFilesCSS (Requires HTML, setting 0)
        features[45] = 0f

        // 47. CntFilesJS (Requires HTML, setting 0)
        features[46] = 0f

        // 48. CntSelfHRef (Requires HTML, setting 0)
        features[47] = 0f

        // 49. CntEmptyRef (Requires HTML, setting 0)
        features[48] = 0f

        // 50. CntExternalRef (Requires HTML, setting 0)
        features[49] = 0f

        // 51. CntPopup (Requires HTML, setting 0)
        features[50] = 0f

        // 52. CntIFrame (Requires HTML, setting 0)
        features[51] = 0f

        // 53. UniqueFeatureCnt
        features[52] = features.take(52).count { it != 0f }.toFloat()

        // 54. WAPLegitimate (Placeholder, setting 0)
        features[53] = 0f

        // 55. WAPPhishing (Placeholder, setting 0)
        features[54] = 0f

        // 56. ShannonEntropy
        features[55] = calculateShannonEntropy(url).toFloat()

        // 57. FractalDimension (Approximate)
        features[56] = features[1] * 1.5f

        // 58. KolmogorovComplexity (Approximate)
        features[57] = (uniqueChars.toFloat() / (url.length.coerceAtLeast(1))) * log2(url.length.toDouble().coerceAtLeast(1.0)).toFloat()

        // 59. HexPatternCnt
        features[58] = countPattern(url, "%[0-9A-Fa-f]{2}")

        // 60. Base64PatternCnt
        features[59] = countPattern(url, "[a-zA-Z0-9+/]{40,}")

        // 61. LikelinessIndex
        features[60] = (features[1] + features[2] + (1 - features[7])) / 3f

        // 62. ExtraFeature
        features[61] = 0f

        return features
    }

    private fun isIpAddress(hostname: String): Boolean {
        val ipPattern = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
        )
        return ipPattern.matcher(hostname).matches()
    }

    private fun containsKeywords(text: String, keywords: List<String>): Boolean {
        val lowerText = text.lowercase()
        return keywords.any { lowerText.contains(it) }
    }

    private fun calculateShannonEntropy(url: String): Double {
        if (url.isEmpty()) return 0.0
        val frequencies = url.groupingBy { it }.eachCount()
        var entropy = 0.0
        for (count in frequencies.values) {
            val probability = count.toDouble() / url.length
            entropy -= probability * log2(probability)
        }
        return entropy
    }

    private fun countPattern(text: String, pattern: String): Float {
        val matcher = Pattern.compile(pattern).matcher(text)
        var count = 0
        while (matcher.find()) count++
        return count.toFloat()
    }
}
