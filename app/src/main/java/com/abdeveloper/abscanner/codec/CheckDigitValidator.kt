package com.abdeveloper.abscanner.codec

object CheckDigitValidator {

    /**
     * Calculates the Modulo 10 check digit for standard GS1 barcodes (EAN-13, EAN-8, UPC-A, ITF-14).
     * The input should be the digits without the final check digit.
     */
    fun computeMod10CheckDigit(digitsWithoutCheck: String): Int? {
        val clean = digitsWithoutCheck.filter { it.isDigit() }
        if (clean.isEmpty()) return null

        var sum = 0
        val isOddLength = clean.length % 2 != 0

        for (i in clean.indices) {
            val d = clean[i].digitToInt()
            val weight = if (isOddLength) {
                if (i % 2 == 0) 3 else 1
            } else {
                if (i % 2 == 0) 1 else 3
            }
            sum += d * weight
        }

        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }

    /**
     * Validates if a complete barcode with check digit has a valid check digit.
     */
    fun isValidMod10(fullBarcode: String): Boolean {
        val clean = fullBarcode.filter { it.isDigit() }
        if (clean.length < 2) return false
        val payload = clean.substring(0, clean.length - 1)
        val expected = computeMod10CheckDigit(payload) ?: return false
        val actual = clean.last().digitToInt()
        return expected == actual
    }

    /**
     * Validates ISBN-10 check digit (modulo 11 with 'X' as 10).
     */
    fun isValidIsbn10(isbn10: String): Boolean {
        val clean = isbn10.replace("-", "").trim().uppercase()
        if (clean.length != 10) return false

        var sum = 0
        for (i in 0 until 9) {
            if (!clean[i].isDigit()) return false
            sum += clean[i].digitToInt() * (10 - i)
        }

        val checkChar = clean[9]
        val checkValue = if (checkChar == 'X') 10 else if (checkChar.isDigit()) checkChar.digitToInt() else return false
        sum += checkValue

        return sum % 11 == 0
    }
}
