package com.billtrack.utils

import android.util.Log
import java.util.Locale

object BillTextParser {

    private const val TAG = "BillTextParser"

    private val TOTAL_KEYWORDS = setOf(
        "TOTAL", "TOTAL DUE", "AMOUNT DUE", "GRAND TOTAL", "BALANCE", "BALANCE DUE",
        "TOTAL AMOUNT", "PAYABLE AMOUNT", "NET TOTAL", "INVOICE TOTAL", "TOTAL CHARGE",
        "TOTAL PAID", "TOTAL TO PAY", "TOTAL OWED", "SUBTOTAL", "SUB TOTAL", "SUMME", "GESAMT"
        // Added SUBTOTAL as it can sometimes be the only 'total-like' figure or the largest.
        // Added German keywords as examples for internationalization.
    )

    // Regex to find potential monetary values during extraction.
    private val MONEY_PATTERN = Regex("""(?i)(?:[A-Z]{3}\s*|\p{Sc}\s*)?(\d{1,3}(?:[,.]\d{3})*(?:[.,]\d{2})|\d+(?:[.,]\d{2})|\d+)(?:\s*[A-Z]{3})?""")

    /**
     * Tries to find the most likely total amount from a block of text recognized from a bill.
     * This version prioritizes the largest number found, especially if associated with a keyword.
     *
     * @param billText The multi-line text extracted from the bill.
     * @return The parsed total as a Double, or null if no likely total is found.
     */
    fun findTotalAmountInText(billText: String): Double? {
        val lines = billText.lines()
        val allAmountsWithContext = mutableListOf<Pair<Double, Boolean>>() // Pair: <Amount, IsKeywordAssociated>

        for (i in lines.indices) {
            val currentLine = lines[i]
            val upperCaseLine = currentLine.uppercase(Locale.ROOT)

            var isKeywordAssociated = false
            // Check current line for keyword
            if (TOTAL_KEYWORDS.any { upperCaseLine.contains(it) }) {
                isKeywordAssociated = true
            }
            
            // Extract amounts from the current line
            val amountsOnCurrentLine = extractAmountsFromText(currentLine)

            // Log.d(TAG, "Amounts on current line '$currentLine': $amountsOnCurrentLine") // Optional: detailed logging
            for (amount in amountsOnCurrentLine) {
                var association = isKeywordAssociated
                // If amount is on a line directly below a keyword line, also consider it associated
                if (!association && i > 0) {
                    if (TOTAL_KEYWORDS.any { lines[i-1].uppercase(Locale.ROOT).contains(it) }) {
                        association = true
                    }
                }
                allAmountsWithContext.add(Pair(amount, association))
            }
        }

        if (allAmountsWithContext.isEmpty()) {
            Log.d(TAG, "No numeric amounts found in the bill text.")
            return null
        }

        val keywordAssociatedAmounts = allAmountsWithContext.filter { it.second }.map { it.first }.distinct()
        val allExtractedAmounts = allAmountsWithContext.map { it.first }.distinct().sortedDescending()

        if (keywordAssociatedAmounts.isNotEmpty()) {
            val largestKeywordAmount = keywordAssociatedAmounts.maxOrNull()!!
            Log.d(TAG, "Largest keyword-associated amount: $largestKeywordAmount")
            return largestKeywordAmount
        }

        if (allExtractedAmounts.isNotEmpty()) {
            val largestOverallAmount = allExtractedAmounts.first()
            Log.d(TAG, "No keyword-associated amount found. Returning largest overall amount: $largestOverallAmount")
            return largestOverallAmount
        }
        
        Log.d(TAG, "Could not determine a total amount.")
        return null
    }

    /**
     * Extracts and cleans potential monetary values from a single line of text.
     * Handles formats like 1,234.56 (US) and 1.234,56 (EU) for parsing.
     */
    private fun extractAmountsFromText(text: String): List<Double> {
        val amounts = mutableListOf<Double>()
        MONEY_PATTERN.findAll(text).forEach { matchResult ->
            var numericString = matchResult.groupValues[1] 

            val dotCount = numericString.count { it == '.' }
            val commaCount = numericString.count { it == ',' }

            if (dotCount > 0 && commaCount > 0) { 
                if (numericString.lastIndexOf('.') > numericString.lastIndexOf(',')) {
                    numericString = numericString.replace(",", "")
                } else {
                    numericString = numericString.replace(".", "").replace(",", ".")
                }
            } else if (dotCount > 1) {
                numericString = numericString.replace(".", "")
            } else if (commaCount > 1) {
                numericString = numericString.replace(",", "")
            } else if (commaCount == 1 && dotCount == 0) {
                if (numericString.substringAfterLast(',').length <= 2) { // check if digits after comma <= 2
                    numericString = numericString.replace(",", ".")
                } else {
                    numericString = numericString.replace(",", "")
                }
            } else if (dotCount == 1 && commaCount == 0) {
                 // If there are more than 2 digits after a single dot, it might be a version or ID, not currency.
                 // However, for extraction, we are more lenient. The final check is the toDouble().
                 // No specific change here for now, handled by toDouble() and sanity checks.
            }

            try {
                val value = numericString.toDouble()
                if (value >= 0 && value < 10_000_000) { 
                    amounts.add(value)
                }
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Could not parse normalized string '$numericString' (from '${matchResult.value}') as double")
            }
        }
        return amounts
    }

    /**
     * Checks if the given string matches common monetary formats like "25,000", "25,000.00", or "25.000,00".
     * @param numString The string to check.
     * @return True if the string matches one of the expected monetary formats, false otherwise.
     */
    fun checkFormat(numString: String): Boolean {
        // This regex handles:
        // 1. Numbers with comma as thousands separator, optional dot and 2 decimal places (e.g., 1,234.56 or 1,234 or 123.45 or 123)
        // 2. Numbers with dot as thousands separator, optional comma and 2 decimal places (e.g., 1.234,56 or 1.234 or 123,45 or 123)
        // 3. Simple numbers with no thousands separators (e.g., 12345.67 or 12345 or 123.45 or 123,45)
        val monetaryRegex = Regex(
            """^(
                \d{1,3}(?:,\d{3})*(?:\.\d{2})?     # Style 1: 1,234.56 or 1,234 or 123 or 123.45
                |
                \d{1,3}(?:\.\d{3})*(?:,\d{2})?     # Style 2: 1.234,56 or 1.234 or 123 or 123,45
                |
                \d+(?:[.,]\d{2})?                 # Style 3: 12345.67 or 12345,67 or 12345 (catches simple numbers too)
                |
                \d+                               # Style 4: Integer only like 25000
            )$""",
            RegexOption.COMMENTS // Allows comments in the regex pattern for readability
        )
        return monetaryRegex.matches(numString)
    }
}
