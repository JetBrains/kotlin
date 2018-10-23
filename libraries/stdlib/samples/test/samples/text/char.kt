package samples.text

import samples.*
import kotlin.test.*
import java.util.*

class Chars {

    @Sample
    fun isLetter() {
        val chars = listOf('a', 'β', '+', '1')
        val (letters, notLetters) = chars.partition { it.isLetter() }
        assertPrints(letters, "[a, β]")
        assertPrints(notLetters, "[+, 1]")
    }

    @Sample
    fun isLetterOrDigit() {
        val chars = listOf('a', '1', '+')
        val (letterOrDigitList, notLetterOrDigitList) = chars.partition { it.isLetterOrDigit() }
        assertPrints(letterOrDigitList, "[a, 1]")
        assertPrints(notLetterOrDigitList, "[+]")
    }

    @Sample
    fun isDigit() {
        val chars = listOf('a', '+', '1')
        val (digits, notDigits) = chars.partition { it.isDigit() }
        assertPrints(digits, "[1]")
        assertPrints(notDigits, "[a, +]")
    }

    @Sample
    fun isISOControl() {
        val chars = listOf('\u0000', '\u000E', '\u0009', '1', 'a')
        val (isoControls, notIsoControls) = chars.partition { it.isISOControl() }
        // some ISO-control char codes
        assertPrints(isoControls.map(Char::toInt), "[0, 14, 9]")
        // non-ISO-control chars
        assertPrints(notIsoControls, "[1, a]")
    }

    @Sample
    fun isJavaIdentifierPart() {
        val chars = listOf('a', '_', '1', 'β', '$', '+', ';')
        val (javaIdentifierParts, notJavaIdentifierParts) = chars.partition { it.isJavaIdentifierPart() }
        assertPrints(javaIdentifierParts, "[a, _, 1, β, $]")
        assertPrints(notJavaIdentifierParts, "[+, ;]")
    }

    @Sample
    fun isJavaIdentifierStart() {
        val chars = listOf('a', '_', 'β', '$', '1', '+', ';')
        val (javaIdentifierStarts, notJavaIdentifierStarts) = chars.partition { it.isJavaIdentifierStart() }
        assertPrints(javaIdentifierStarts, "[a, _, β, $]")
        assertPrints(notJavaIdentifierStarts, "[1, +, ;]")
    }

    @Sample
    fun isWhitespace() {
        val chars = listOf(' ', '\t', '\n', '1', 'a', '\u00A0')
        val (whitespaces, notWhitespaces) = chars.partition { it.isWhitespace() }
        // whitespace char codes
        assertPrints(whitespaces.map(Char::toInt), "[32, 9, 10, 160]")
        // non-whitespace chars
        assertPrints(notWhitespaces, "[1, a]")
    }

    @Sample
    fun isUpperCase() {
        val chars = listOf('A', 'Ψ', 'a', '1', '+')
        val (upperCases, notUpperCases) = chars.partition { it.isUpperCase() }
        assertPrints(upperCases, "[A, Ψ]")
        assertPrints(notUpperCases, "[a, 1, +]")
    }

    @Sample
    fun isLowerCase() {
        val chars = listOf('a', 'λ', 'A', '1', '+')
        val (lowerCases, notLowerCases) = chars.partition { it.isLowerCase() }
        assertPrints(lowerCases, "[a, λ]")
        assertPrints(notLowerCases, "[A, 1, +]")
    }

    @Sample
    fun toUpperCase() {
        val chars = listOf('a', 'ω', '1', 'A', '+')
        val upperCases = chars.map { it.toUpperCase() }
        assertPrints(upperCases, "[A, Ω, 1, A, +]")
    }

    @Sample
    fun toLowerCase() {
        val chars = listOf('A', 'Ω', '1', 'a', '+')
        val lowerCases = chars.map { it.toLowerCase() }
        assertPrints(lowerCases, "[a, ω, 1, a, +]")
    }

    @Sample
    fun isTitleCase() {
        val chars = listOf('ǅ', 'ǈ', 'ǋ', 'ǲ', '1', 'A', 'a', '+')
        val (titleCases, notTitleCases) = chars.partition { it.isTitleCase() }
        assertPrints(titleCases, "[ǅ, ǈ, ǋ, ǲ]")
        assertPrints(notTitleCases, "[1, A, a, +]")
    }

    @Sample
    fun toTitleCase() {
        val chars = listOf('a', 'ǅ', '1', '+')
        val titleCases = chars.map { it.toTitleCase() }
        assertPrints(titleCases, "[A, ǅ, 1, +]")
    }

    @Sample
    fun plus() {
        val value = 'a' + "bcd"
        assertPrints(value, "abcd")
    }

    @Sample
    fun equals() {
        assertTrue('a'.equals('a', false))
        assertFalse('a'.equals('A', false))
        assertTrue('a'.equals('A', true))
    }

}