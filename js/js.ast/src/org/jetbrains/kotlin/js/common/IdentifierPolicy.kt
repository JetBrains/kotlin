package org.jetbrains.kotlin.js.common

private fun Char.isAllowedLatinLetterOrSpecial(): Boolean {
    return this in 'a'..'z' || this in 'A'..'Z' || this == '_' || this == '$'
}

private fun Char.isAllowedSimpleDigit() =
    this in '0'..'9'

private fun Char.isNotAllowedSimpleCharacter() = when (this) {
    ' ', '<', '>', '-', '?' -> true
    else -> false
}

fun Char.isES5IdentifierStart(): Boolean {
    if (isAllowedLatinLetterOrSpecial()) return true
    if (isNotAllowedSimpleCharacter()) return false

    return isES5IdentifierStartFull()
}

// See ES 5.1 spec: https://www.ecma-international.org/ecma-262/5.1/#sec-7.6
private fun Char.isES5IdentifierStartFull() =
    Character.isLetter(this) ||   // Lu | Ll | Lt | Lm | Lo
            // Nl which is missing in Character.isLetter, but present in UnicodeLetter in spec
            Character.getType(this).toByte() == Character.LETTER_NUMBER


fun Char.isES5IdentifierPart(): Boolean {
    if (isAllowedLatinLetterOrSpecial()) return true
    if (isAllowedSimpleDigit()) return true
    if (isNotAllowedSimpleCharacter()) return false

    return isES5IdentifierStartFull() ||
            when (Character.getType(this).toByte()) {
                Character.NON_SPACING_MARK,
                Character.COMBINING_SPACING_MARK,
                Character.DECIMAL_DIGIT_NUMBER,
                Character.CONNECTOR_PUNCTUATION -> true
                else -> false
            } ||
            this == '\u200C' ||   // Zero-width non-joiner
            this == '\u200D'      // Zero-width joiner
}

fun String.isValidES5Identifier(): Boolean {
    if (isEmpty() || !this[0].isES5IdentifierStart()) return false
    for (idx in 1 until length) {
        if (!get(idx).isES5IdentifierPart()) return false
    }
    return true
}