package com.abdeveloper.abscanner.codec

import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

object UrlRiskAnalyzer {

    private val IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    private val KNOWN_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "buff.ly", "ow.ly",
        "goo.gl", "rebrand.ly", "cutt.ly", "shorturl.at", "bl.ink", "tiny.cc"
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "work", "click", "gq", "tk", "ml", "cf", "buzz", "rest", "zip", "mov", "cam"
    )

    fun analyze(urlStr: String): UrlRisk {
        val warnings = mutableListOf<String>()
        var isHttps = false
        var host = ""
        var isIp = false
        var isShortener = false

        try {
            val uri = URI(urlStr)
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
            isHttps = scheme == "https"
            host = uri.host?.lowercase(Locale.ROOT) ?: ""

            if (!isHttps && scheme == "http") {
                warnings.add("Unencrypted HTTP connection. Sensitive data can be intercepted.")
            }

            if (uri.userInfo != null && uri.userInfo.isNotEmpty()) {
                warnings.add("Contains embedded credentials in URL, often used in phishing lures.")
            }

            if (host.isNotEmpty()) {
                if (IP_PATTERN.matcher(host).matches()) {
                    isIp = true
                    warnings.add("Destination is a raw IP address instead of a recognized domain name.")
                }

                if (KNOWN_SHORTENERS.contains(host)) {
                    isShortener = true
                    warnings.add("URL shortener detected. The actual destination target is concealed.")
                }

                val parts = host.split(".")
                if (parts.size >= 2) {
                    val tld = parts.last()
                    if (SUSPICIOUS_TLDS.contains(tld)) {
                        warnings.add("Uses top-level domain (.$tld) frequently associated with spam or phishing.")
                    }
                }

                if (parts.size > 4 && !isIp) {
                    warnings.add("High number of subdomains detected (${parts.size - 2}), common in spoofing attacks.")
                }

                if (host.contains("@") || host.contains("%40")) {
                    warnings.add("Host name contains suspicious symbol obfuscation.")
                }
            } else {
                warnings.add("Unable to parse host address reliably.")
            }
        } catch (_: Exception) {
            warnings.add("Malformed URL structure.")
        }

        val level = when {
            isIp || warnings.size >= 2 -> RiskLevel.DANGER
            warnings.isNotEmpty() -> RiskLevel.WARNING
            else -> RiskLevel.SAFE
        }

        return UrlRisk(
            level = level,
            warnings = warnings,
            isHttps = isHttps,
            host = host,
            isIpAddress = isIp,
            isShortener = isShortener
        )
    }
}
