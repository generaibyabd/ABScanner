package com.abdeveloper.abscanner.codec

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern

object CodeParser {

    fun parse(rawValue: String, formatName: String): ParsedCode {
        val trimmed = rawValue.trim()

        // 1. Wi-Fi
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            return parseWifi(trimmed, formatName)
        }

        // 2. vCard / MeCard / BizCard Contact
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) ||
            trimmed.startsWith("MECARD:", ignoreCase = true) ||
            trimmed.startsWith("BIZCARD:", ignoreCase = true)
        ) {
            return parseContact(trimmed, formatName)
        }

        // 3. iCalendar Event
        if (trimmed.startsWith("BEGIN:VEVENT", ignoreCase = true) ||
            trimmed.startsWith("BEGIN:VCALENDAR", ignoreCase = true)
        ) {
            return parseCalendar(trimmed, formatName)
        }

        // 4. UPI Payment
        if (trimmed.startsWith("upi://pay", ignoreCase = true)) {
            return parseUpi(trimmed, formatName)
        }

        // 5. SEPA EPC Payment
        if (trimmed.startsWith("BCD\n") || trimmed.startsWith("BCD\r\n")) {
            return parseSepa(trimmed, formatName)
        }

        // 6. Crypto (Bitcoin, Ethereum, etc.)
        if (trimmed.startsWith("bitcoin:", ignoreCase = true) ||
            trimmed.startsWith("ethereum:", ignoreCase = true) ||
            trimmed.startsWith("litecoin:", ignoreCase = true) ||
            trimmed.startsWith("solana:", ignoreCase = true)
        ) {
            return parseCrypto(trimmed, formatName)
        }

        // 7. Phone (tel:)
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.substring(4)
            return ParsedCode(
                rawValue = rawValue,
                formatName = formatName,
                type = CodeType.PHONE,
                title = phone,
                subtitle = "Telephone Dial",
                fields = listOf(ParsedField("Phone Number", phone)),
                primaryActionTitle = "Call Number",
                primaryActionIntentUri = trimmed
            )
        }

        // 8. SMS (sms: or SMSTO:)
        if (trimmed.startsWith("sms:", ignoreCase = true) || trimmed.startsWith("SMSTO:", ignoreCase = true)) {
            return parseSms(trimmed, formatName)
        }

        // 9. Email (mailto: or MATMSG:)
        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            return parseEmail(trimmed, formatName)
        }

        // 10. Geo location (geo:lat,lng)
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            return parseGeo(trimmed, formatName)
        }

        // 11. WhatsApp link (wa.me)
        if (trimmed.startsWith("https://wa.me/", ignoreCase = true) ||
            trimmed.startsWith("whatsapp://send", ignoreCase = true)
        ) {
            return ParsedCode(
                rawValue = rawValue,
                formatName = formatName,
                type = CodeType.WHATSAPP,
                title = "WhatsApp Chat",
                subtitle = trimmed,
                fields = listOf(ParsedField("WhatsApp Link", trimmed)),
                primaryActionTitle = "Open in WhatsApp",
                primaryActionIntentUri = trimmed
            )
        }

        // 12. Telegram link (t.me)
        if (trimmed.startsWith("https://t.me/", ignoreCase = true) ||
            trimmed.startsWith("tg://resolve", ignoreCase = true)
        ) {
            return ParsedCode(
                rawValue = rawValue,
                formatName = formatName,
                type = CodeType.TELEGRAM,
                title = "Telegram Link",
                subtitle = trimmed,
                fields = listOf(ParsedField("Telegram Link", trimmed)),
                primaryActionTitle = "Open Telegram",
                primaryActionIntentUri = trimmed
            )
        }

        // 13. App Store (market:// or Google Play Store)
        if (trimmed.startsWith("market://", ignoreCase = true) ||
            (trimmed.contains("play.google.com/store/apps/details", ignoreCase = true))
        ) {
            return ParsedCode(
                rawValue = rawValue,
                formatName = formatName,
                type = CodeType.APP_STORE,
                title = "Google Play App",
                subtitle = trimmed,
                fields = listOf(ParsedField("Store Link", trimmed)),
                primaryActionTitle = "Open in Play Store",
                primaryActionIntentUri = trimmed
            )
        }

        // 14. Standard Web URLs
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val risk = UrlRiskAnalyzer.analyze(trimmed)
            return ParsedCode(
                rawValue = rawValue,
                formatName = formatName,
                type = CodeType.URL,
                title = risk.host.ifEmpty { trimmed },
                subtitle = trimmed,
                fields = listOf(
                    ParsedField("Protocol", if (risk.isHttps) "HTTPS (Encrypted)" else "HTTP (Unencrypted)"),
                    ParsedField("Host", risk.host),
                    ParsedField("Full URL", trimmed)
                ),
                primaryActionTitle = "Open Link",
                primaryActionIntentUri = trimmed,
                urlRisk = risk
            )
        }

        // 15. Barcodes: EAN-13, EAN-8, UPC-A, UPC-E, ITF-14, ISBN
        val cleanDigits = trimmed.filter { it.isDigit() }
        if (cleanDigits.length in listOf(8, 12, 13, 14) && (cleanDigits == trimmed || trimmed.matches(Regex("^[0-9\\-]+$")))) {
            val gs1Country = Gs1PrefixDirectory.lookupCountryOrCategory(cleanDigits)
            val isValidCheck = CheckDigitValidator.isValidMod10(cleanDigits)

            // Check if Book ISBN (starts with 978 or 979)
            if (cleanDigits.length == 13 && (cleanDigits.startsWith("978") || cleanDigits.startsWith("979"))) {
                return ParsedCode(
                    rawValue = rawValue,
                    formatName = formatName.ifEmpty { "EAN-13 / ISBN" },
                    type = CodeType.ISBN,
                    title = "Book (ISBN-13)",
                    subtitle = cleanDigits,
                    fields = listOf(
                        ParsedField("ISBN Number", cleanDigits),
                        ParsedField("GS1 Group", gs1Country ?: "Bookland"),
                        ParsedField("Check Digit", if (isValidCheck) "Valid (Verified)" else "Invalid")
                    ),
                    primaryActionTitle = "Search Book Online",
                    primaryActionIntentUri = "https://www.google.com/search?q=ISBN+$cleanDigits",
                    gs1Description = gs1Country
                )
            }

            return ParsedCode(
                rawValue = rawValue,
                formatName = formatName.ifEmpty { if (cleanDigits.length == 13) "EAN-13" else if (cleanDigits.length == 8) "EAN-8" else "UPC" },
                type = CodeType.PRODUCT,
                title = "Product Barcode ($cleanDigits)",
                subtitle = gs1Country ?: "Product / GTIN",
                fields = listOf(
                    ParsedField("Barcode Number", cleanDigits),
                    ParsedField("Origin / Organization", gs1Country ?: "Unknown GS1 Territory"),
                    ParsedField("Checksum", if (isValidCheck) "Valid Modulo-10" else "Invalid Check Digit")
                ),
                primaryActionTitle = "Search Product Info",
                primaryActionIntentUri = "https://world.openfoodfacts.org/product/$cleanDigits",
                gs1Description = gs1Country
            )
        }

        // 16. GS1 Application Identifiers
        if (trimmed.startsWith("(") && trimmed.contains(")")) {
            val parsedFields = parseGs1ApplicationIdentifiers(trimmed)
            if (parsedFields.isNotEmpty()) {
                return ParsedCode(
                    rawValue = rawValue,
                    formatName = formatName.ifEmpty { "GS1 Composite" },
                    type = CodeType.GS1_DATA,
                    title = "GS1 Composite Data",
                    subtitle = "${parsedFields.size} standard AI fields parsed",
                    fields = parsedFields,
                    primaryActionTitle = "Copy GS1 Data"
                )
            }
        }

        // Default Plain Text
        val isSingleLine = !trimmed.contains("\n")
        return ParsedCode(
            rawValue = rawValue,
            formatName = formatName.ifEmpty { "Text" },
            type = if (isSingleLine && trimmed.length <= 100) CodeType.TEXT else CodeType.RAW,
            title = if (trimmed.length > 50) trimmed.take(50) + "..." else trimmed,
            subtitle = "${trimmed.length} characters",
            fields = listOf(ParsedField("Content", trimmed)),
            primaryActionTitle = "Search Web",
            primaryActionIntentUri = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        )
    }

    private fun parseWifi(raw: String, formatName: String): ParsedCode {
        // Format: WIFI:S:MySSID;T:WPA;P:MyPassword;H:false;;
        var ssid = ""
        var type = "WPA"
        var password = ""
        var hidden = false

        val content = raw.substring(5)
        val tokens = splitEscaped(content, ';')
        for (token in tokens) {
            when {
                token.startsWith("S:") -> ssid = unescape(token.substring(2))
                token.startsWith("T:") -> type = token.substring(2).uppercase()
                token.startsWith("P:") -> password = unescape(token.substring(2))
                token.startsWith("H:") -> hidden = token.substring(2).equals("true", ignoreCase = true)
            }
        }

        val fields = mutableListOf(
            ParsedField("Network (SSID)", ssid),
            ParsedField("Security", if (type.isEmpty() || type == "NOPASS") "Open (No Password)" else type),
            ParsedField("Password", password, isSensitive = true),
            ParsedField("Hidden Network", if (hidden) "Yes" else "No")
        )

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.WIFI,
            title = ssid.ifEmpty { "Wi-Fi Network" },
            subtitle = if (type == "NOPASS") "Open Network" else "Secured with $type",
            fields = fields,
            primaryActionTitle = "Connect to Wi-Fi",
            primaryActionIntentUri = null // Handled through Android Wifi suggestion / network settings
        )
    }

    private fun parseContact(raw: String, formatName: String): ParsedCode {
        var name = ""
        var phone = ""
        var email = ""
        var org = ""
        var address = ""
        var url = ""

        if (raw.startsWith("MECARD:", ignoreCase = true)) {
            val content = raw.substring(7)
            val tokens = splitEscaped(content, ';')
            for (token in tokens) {
                when {
                    token.startsWith("N:") -> name = token.substring(2)
                    token.startsWith("TEL:") -> phone = token.substring(4)
                    token.startsWith("EMAIL:") -> email = token.substring(6)
                    token.startsWith("ORG:") -> org = token.substring(4)
                    token.startsWith("ADR:") -> address = token.substring(4)
                    token.startsWith("URL:") -> url = token.substring(4)
                }
            }
        } else {
            // vCard lines
            val lines = raw.lines()
            for (line in lines) {
                val upper = line.uppercase(Locale.ROOT)
                when {
                    upper.startsWith("FN:") -> name = line.substring(3).trim()
                    upper.startsWith("N:") && name.isEmpty() -> name = line.substring(2).replace(";", " ").trim()
                    upper.startsWith("TEL") -> phone = line.substringAfter(":").trim()
                    upper.startsWith("EMAIL") -> email = line.substringAfter(":").trim()
                    upper.startsWith("ORG:") -> org = line.substring(4).trim()
                    upper.startsWith("ADR") -> address = line.substringAfter(":").replace(";", " ").trim()
                    upper.startsWith("URL:") -> url = line.substring(4).trim()
                }
            }
        }

        val fields = mutableListOf<ParsedField>()
        if (name.isNotEmpty()) fields.add(ParsedField("Full Name", name))
        if (phone.isNotEmpty()) fields.add(ParsedField("Phone", phone))
        if (email.isNotEmpty()) fields.add(ParsedField("Email", email))
        if (org.isNotEmpty()) fields.add(ParsedField("Company / Organization", org))
        if (address.isNotEmpty()) fields.add(ParsedField("Address", address))
        if (url.isNotEmpty()) fields.add(ParsedField("Website", url))

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.CONTACT,
            title = name.ifEmpty { phone.ifEmpty { "Contact Card" } },
            subtitle = org.ifEmpty { phone },
            fields = fields,
            primaryActionTitle = "Add to Contacts"
        )
    }

    private fun parseCalendar(raw: String, formatName: String): ParsedCode {
        var summary = ""
        var start = ""
        var end = ""
        var location = ""
        var description = ""

        val lines = raw.lines()
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            when {
                upper.startsWith("SUMMARY:") -> summary = line.substring(8).trim()
                upper.startsWith("DTSTART:") -> start = line.substring(8).trim()
                upper.startsWith("DTEND:") -> end = line.substring(6).trim()
                upper.startsWith("LOCATION:") -> location = line.substring(9).trim()
                upper.startsWith("DESCRIPTION:") -> description = line.substring(12).trim()
            }
        }

        val fields = mutableListOf<ParsedField>()
        if (summary.isNotEmpty()) fields.add(ParsedField("Event Title", summary))
        if (start.isNotEmpty()) fields.add(ParsedField("Start Time", start))
        if (end.isNotEmpty()) fields.add(ParsedField("End Time", end))
        if (location.isNotEmpty()) fields.add(ParsedField("Location", location))
        if (description.isNotEmpty()) fields.add(ParsedField("Details", description))

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.CALENDAR,
            title = summary.ifEmpty { "Calendar Event" },
            subtitle = start.ifEmpty { location },
            fields = fields,
            primaryActionTitle = "Add to Calendar"
        )
    }

    private fun parseUpi(raw: String, formatName: String): ParsedCode {
        // upi://pay?pa=address@bank&pn=Merchant%20Name&am=150.00&cu=INR
        val fields = mutableListOf<ParsedField>()
        var payee = ""
        var name = ""
        var amount = ""
        var currency = "INR"

        try {
            val query = raw.substringAfter("?", "")
            for (param in query.split("&")) {
                val parts = param.split("=")
                if (parts.size == 2) {
                    val k = parts[0]
                    val v = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                    when (k) {
                        "pa" -> payee = v
                        "pn" -> name = v
                        "am" -> amount = v
                        "cu" -> currency = v
                    }
                }
            }
        } catch (_: Exception) {}

        if (payee.isNotEmpty()) fields.add(ParsedField("UPI ID", payee))
        if (name.isNotEmpty()) fields.add(ParsedField("Payee Name", name))
        if (amount.isNotEmpty()) fields.add(ParsedField("Amount", "$currency $amount"))

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.PAYMENT_UPI,
            title = if (amount.isNotEmpty()) "$currency $amount to ${name.ifEmpty { payee }}" else "UPI Payment (${name.ifEmpty { payee }})",
            subtitle = payee,
            fields = fields,
            primaryActionTitle = "Pay with UPI",
            primaryActionIntentUri = raw
        )
    }

    private fun parseSepa(raw: String, formatName: String): ParsedCode {
        // EPC QR Code lines: BCD, version, charset, identification, bic, name, iban, amount, purpose, remittance
        val lines = raw.lines()
        var iban = ""
        var name = ""
        var bic = ""
        var amount = ""
        var remittance = ""

        if (lines.size >= 8) {
            bic = lines.getOrNull(4) ?: ""
            name = lines.getOrNull(5) ?: ""
            iban = lines.getOrNull(6) ?: ""
            amount = lines.getOrNull(7)?.replace("EUR", "€ ") ?: ""
            remittance = lines.getOrNull(9) ?: ""
        }

        val fields = listOf(
            ParsedField("Recipient", name),
            ParsedField("IBAN", iban),
            ParsedField("BIC", bic),
            ParsedField("Amount", amount),
            ParsedField("Reference / Remittance", remittance)
        ).filter { it.value.isNotEmpty() }

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.PAYMENT_SEPA,
            title = "SEPA Credit Transfer",
            subtitle = if (amount.isNotEmpty()) "$amount to $name" else name,
            fields = fields,
            primaryActionTitle = "Copy IBAN",
            primaryActionIntentUri = null
        )
    }

    private fun parseCrypto(raw: String, formatName: String): ParsedCode {
        val scheme = raw.substringBefore(":")
        val address = raw.substringAfter(":").substringBefore("?")
        val fields = listOf(
            ParsedField("Cryptocurrency", scheme.uppercase()),
            ParsedField("Wallet Address", address)
        )

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.PAYMENT_CRYPTO,
            title = "${scheme.uppercase()} Address",
            subtitle = address,
            fields = fields,
            primaryActionTitle = "Copy Address",
            primaryActionIntentUri = raw
        )
    }

    private fun parseSms(raw: String, formatName: String): ParsedCode {
        var recipient = ""
        var body = ""

        if (raw.startsWith("SMSTO:", ignoreCase = true)) {
            val content = raw.substring(6)
            recipient = content.substringBefore(":")
            body = content.substringAfter(":", "")
        } else {
            val query = raw.substringAfter("?", "")
            recipient = raw.substring(4).substringBefore("?")
            if (query.contains("body=")) {
                body = URLDecoder.decode(query.substringAfter("body="), StandardCharsets.UTF_8.name())
            }
        }

        val fields = mutableListOf(ParsedField("Recipient", recipient))
        if (body.isNotEmpty()) fields.add(ParsedField("Message Content", body))

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.SMS,
            title = "SMS to $recipient",
            subtitle = body.ifEmpty { "Send SMS message" },
            fields = fields,
            primaryActionTitle = "Send SMS",
            primaryActionIntentUri = "sms:$recipient"
        )
    }

    private fun parseEmail(raw: String, formatName: String): ParsedCode {
        var to = ""
        var subject = ""
        var body = ""

        if (raw.startsWith("MATMSG:", ignoreCase = true)) {
            val content = raw.substring(7)
            for (token in splitEscaped(content, ';')) {
                when {
                    token.startsWith("TO:") -> to = token.substring(3)
                    token.startsWith("SUB:") -> subject = token.substring(4)
                    token.startsWith("BODY:") -> body = token.substring(5)
                }
            }
        } else {
            val parts = raw.substring(7).split("?")
            to = parts[0]
            if (parts.size > 1) {
                for (param in parts[1].split("&")) {
                    val kv = param.split("=")
                    if (kv.size == 2) {
                        val v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name())
                        if (kv[0].equals("subject", ignoreCase = true)) subject = v
                        if (kv[0].equals("body", ignoreCase = true)) body = v
                    }
                }
            }
        }

        val fields = mutableListOf(ParsedField("To", to))
        if (subject.isNotEmpty()) fields.add(ParsedField("Subject", subject))
        if (body.isNotEmpty()) fields.add(ParsedField("Body", body))

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.EMAIL,
            title = if (subject.isNotEmpty()) subject else "Email to $to",
            subtitle = to,
            fields = fields,
            primaryActionTitle = "Send Email",
            primaryActionIntentUri = "mailto:$to"
        )
    }

    private fun parseGeo(raw: String, formatName: String): ParsedCode {
        val coords = raw.substring(4).substringBefore("?")
        val query = raw.substringAfter("?q=", "")
        val fields = mutableListOf(ParsedField("Coordinates", coords))
        if (query.isNotEmpty()) fields.add(ParsedField("Query / Label", URLDecoder.decode(query, StandardCharsets.UTF_8.name())))

        return ParsedCode(
            rawValue = raw,
            formatName = formatName,
            type = CodeType.GEO,
            title = "Location ($coords)",
            subtitle = query.ifEmpty { coords },
            fields = fields,
            primaryActionTitle = "Open in Maps",
            primaryActionIntentUri = raw
        )
    }

    private fun parseGs1ApplicationIdentifiers(raw: String): List<ParsedField> {
        val fields = mutableListOf<ParsedField>()
        val matcher = Pattern.compile("\\(([0-9]{2,4})\\)([^\\(]+)").matcher(raw)
        while (matcher.find()) {
            val ai = matcher.group(1) ?: ""
            val value = matcher.group(2) ?: ""
            val desc = Gs1PrefixDirectory.lookupGs1ApplicationIdentifier(ai) ?: "AI ($ai)"
            fields.add(ParsedField(desc, value.trim()))
        }
        return fields
    }

    private fun splitEscaped(str: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var escaped = false
        for (c in str) {
            if (escaped) {
                sb.append(c)
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == delimiter) {
                if (sb.isNotEmpty()) {
                    result.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) result.add(sb.toString())
        return result
    }

    private fun unescape(s: String): String {
        return s.replace("\\;", ";").replace("\\:", ":").replace("\\\\", "\\")
    }
}
