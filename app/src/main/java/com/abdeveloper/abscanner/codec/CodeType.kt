package com.abdeveloper.abscanner.codec

enum class CodeType(val displayName: String) {
    URL("Website URL"),
    WIFI("Wi-Fi Network"),
    CONTACT("Contact Card"),
    CALENDAR("Calendar Event"),
    PHONE("Phone Number"),
    SMS("SMS Message"),
    EMAIL("Email Address"),
    GEO("Location Coordinates"),
    PRODUCT("Product Barcode"),
    ISBN("Book (ISBN)"),
    PAYMENT_SEPA("SEPA Payment"),
    PAYMENT_UPI("UPI Payment"),
    PAYMENT_CRYPTO("Crypto Address"),
    WHATSAPP("WhatsApp Chat"),
    TELEGRAM("Telegram Link"),
    APP_STORE("App Store Link"),
    GS1_DATA("GS1 Composite Data"),
    TEXT("Plain Text"),
    RAW("Raw Data")
}

data class ParsedField(
    val label: String,
    val value: String,
    val isSensitive: Boolean = false
)

data class ParsedCode(
    val rawValue: String,
    val formatName: String,
    val type: CodeType,
    val title: String,
    val subtitle: String? = null,
    val fields: List<ParsedField> = emptyList(),
    val primaryActionTitle: String? = null,
    val primaryActionIntentUri: String? = null,
    val urlRisk: UrlRisk? = null,
    val gs1Description: String? = null
)

enum class RiskLevel {
    SAFE,
    WARNING,
    DANGER
}

data class UrlRisk(
    val level: RiskLevel,
    val warnings: List<String>,
    val isHttps: Boolean,
    val host: String,
    val isIpAddress: Boolean,
    val isShortener: Boolean
)
