package samples.text

import samples.*
import kotlin.test.*
import java.util.*

class Chars {

    @Sample
    fun isDefined() {
        assertTrue('$'.isDefined())
    }

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
    fun isIdentifierIgnorable() {
        val chars = listOf('\u0000', '\u000E', '\u0009', '1', 'a')
        val (identifierIgnorables, notIdentifierIgnorables) = chars.partition { it.isIdentifierIgnorable() }
        assertPrints(identifierIgnorables, "[\u0000, \u000E]")
        assertPrints(notIdentifierIgnorables, "[\u0009, 1, a]")
    }

    @Sample
    fun isISOControl() {
        val chars = listOf('\u0000', '\u000E', '\u0009', '1', 'a')
        val (isoControls, notIsoControls) = chars.partition { it.isISOControl() }
        assertPrints(isoControls, "[\u0000, \u000E, \u0009]")
        assertPrints(notIsoControls, "[1, a]")
    }

    @Sample
    fun isJavaIdentifierPart() {
        val chars = listOf('a', '1', 'β', '$', '+', ';')
        val (javaIdentifierParts, notJavaIdentifierParts) = chars.partition { it.isJavaIdentifierPart() }
        assertPrints(javaIdentifierParts, "[a, 1, β, $]")
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
        val chars = listOf(' ', '\t', '1', 'a')
        val (whitespaces, notWhitespaces) = chars.partition { it.isWhitespace() }
        assertPrints(whitespaces, "[ , \t]")
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
    fun isHighSurrogate() {
        val chars = listOf('\uD800', '\uDBFF', 'A', 'a', '1')
        val (highSurrogates, notHighSurrogates) = chars.partition { it.isHighSurrogate() }
        assertPrints(highSurrogates, "[\uD800, \uDBFF]")
        assertPrints(notHighSurrogates, "[A, a, 1]")
    }

    @Sample
    fun isLowSurrogate() {
        val chars = listOf('\uDC00', '\uDFFF', 'A', 'a', '1')
        val (lowSurrogates, notLowSurrogates) = chars.partition { it.isLowSurrogate() }
        assertPrints(lowSurrogates, "[\uDC00, \uDFFF]")
        assertPrints(notLowSurrogates, "[A, a, 1]")
    }

    @Sample
    fun isSurrogate() {
        val chars = listOf('\uDC00', '\uDBFF', 'A', 'a', '1')
        val (surrogates, notSurrogates) = chars.partition { it.isSurrogate() }
        assertPrints(surrogates, "[\uDC00, \uDBFF]")
        assertPrints(notSurrogates, "[A, a, 1]")
    }

    @Sample
    fun plus() {
        val value = 'a' + "bcd"
        assertPrints(value, "abcd")
    }

    @Sample
    fun equals() {
        assertTrue('a'.equals('a', false))
        assertTrue('a'.equals('A', true))
        assertFalse('a'.equals('A', false))
    }

}