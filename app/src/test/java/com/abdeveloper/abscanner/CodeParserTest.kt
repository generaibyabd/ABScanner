package com.abdeveloper.abscanner

import com.abdeveloper.abscanner.codec.CheckDigitValidator
import com.abdeveloper.abscanner.codec.CodeParser
import com.abdeveloper.abscanner.codec.CodeType
import com.abdeveloper.abscanner.codec.Gs1PrefixDirectory
import com.abdeveloper.abscanner.codec.RiskLevel
import com.abdeveloper.abscanner.codec.UrlRiskAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeParserTest {

    @Test
    fun testWifiParsing() {
        val raw = "WIFI:S:HomeNetwork;T:WPA;P:SecretPass123;;"
        val parsed = CodeParser.parse(raw, "QR Code")

        assertEquals(CodeType.WIFI, parsed.type)
        assertEquals("HomeNetwork", parsed.title)
        val passField = parsed.fields.find { it.label == "Password" }
        assertNotNull(passField)
        assertEquals("SecretPass123", passField?.value)
        assertTrue(passField?.isSensitive == true)
    }

    @Test
    fun testVCardParsing() {
        val raw = "BEGIN:VCARD\nVERSION:3.0\nFN:John Doe\nTEL:+15551234567\nEMAIL:john@example.com\nORG:Acme Corp\nEND:VCARD"
        val parsed = CodeParser.parse(raw, "QR Code")

        assertEquals(CodeType.CONTACT, parsed.type)
        assertEquals("John Doe", parsed.title)
        assertEquals("Acme Corp", parsed.subtitle)
        assertEquals("+15551234567", parsed.fields.find { it.label == "Phone" }?.value)
    }

    @Test
    fun testUrlParsingAndRisk() {
        val safeUrl = "https://www.google.com"
        val parsedSafe = CodeParser.parse(safeUrl, "QR Code")
        assertEquals(CodeType.URL, parsedSafe.type)
        assertEquals(RiskLevel.SAFE, parsedSafe.urlRisk?.level)

        val ipUrl = "http://192.168.1.1/admin"
        val parsedIp = CodeParser.parse(ipUrl, "QR Code")
        assertEquals(CodeType.URL, parsedIp.type)
        assertEquals(RiskLevel.DANGER, parsedIp.urlRisk?.level)
        assertTrue(parsedIp.urlRisk?.isIpAddress == true)

        val shortenerUrl = "https://bit.ly/xyz123"
        val parsedShort = CodeParser.parse(shortenerUrl, "QR Code")
        assertTrue(parsedShort.urlRisk?.isShortener == true)
    }

    @Test
    fun testCheckDigitMod10() {
        // Test standard EAN-13
        // 4006381333931 (Stabilo Boss highlighter)
        // Payload: 400638133393 -> Check digit: 1
        val check = CheckDigitValidator.computeMod10CheckDigit("400638133393")
        assertEquals(1, check)
        assertTrue(CheckDigitValidator.isValidMod10("4006381333931"))
        assertFalse(CheckDigitValidator.isValidMod10("4006381333932"))
    }

    @Test
    fun testGs1PrefixDirectory() {
        val germanCountry = Gs1PrefixDirectory.lookupCountryOrCategory("4006381333931")
        assertEquals("GS1 Germany", germanCountry)

        val bookland = Gs1PrefixDirectory.lookupCountryOrCategory("9780132350884")
        assertEquals("Bookland (ISBN)", bookland)
    }

    @Test
    fun testUpiPayment() {
        val upiRaw = "upi://pay?pa=merchant@okaxis&pn=CoffeeShop&am=4.50&cu=EUR"
        val parsed = CodeParser.parse(upiRaw, "QR Code")
        assertEquals(CodeType.PAYMENT_UPI, parsed.type)
        assertEquals("merchant@okaxis", parsed.subtitle)
    }
}
