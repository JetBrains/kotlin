/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.*

class Chars {

    @Sample
    fun isDefined() {
        assertPrints('$'.isDefined(), "true")
    }

    @Sample
    fun isLetter() {
        assertPrints('a'.isLetter(), "true")
        assertPrints('β'.isLetter(), "true")

        assertPrints('+'.isLetter(), "false")
        assertPrints('1'.isLetter(), "false")
    }

    @Sample
    fun isLetterOrDigit() {
        assertPrints('a'.isLetterOrDigit(), "true")
        assertPrints('1'.isLetterOrDigit(), "true")

        assertPrints('+'.isLetterOrDigit(), "false")
    }

    @Sample
    fun isDigit() {
        assertPrints('1'.isDigit(), "true")

        assertPrints('a'.isDigit(), "false")
        assertPrints('+'.isDigit(), "false")
    }


    @Sample
    fun isIdentifierIgnorable() {
        assertPrints('\u0000'.isIdentifierIgnorable(), "true")
        assertPrints('\u000E'.isIdentifierIgnorable(), "true")

        assertPrints('\u0009'.isIdentifierIgnorable(), "false")
        assertPrints('1'.isIdentifierIgnorable(), "false")
        assertPrints('a'.isIdentifierIgnorable(), "false")
    }


    @Sample
    fun isISOControl() {
        assertPrints('\u0000'.isISOControl(), "true")
        assertPrints('\u000E'.isISOControl(), "true")
        assertPrints('\u0009'.isISOControl(), "true")

        assertPrints('1'.isISOControl(), "false")
        assertPrints('a'.isISOControl(), "false")
    }

    @Sample
    fun isJavaIdentifierPart() {
        assertPrints('a'.isJavaIdentifierPart(), "true")
        assertPrints('1'.isJavaIdentifierPart(), "true")
        assertPrints('β'.isJavaIdentifierPart(), "true")

        assertPrints('$'.isJavaIdentifierPart(), "false")
        assertPrints('+'.isJavaIdentifierPart(), "false")
        assertPrints(';'.isJavaIdentifierPart(), "false")
    }

    @Sample
    fun isJavaIdentifierStart() {
        assertPrints('a'.isJavaIdentifierStart(), "true")
        assertPrints('β'.isJavaIdentifierStart(), "true")
        assertPrints('_'.isJavaIdentifierStart(), "true")
        assertPrints('$'.isJavaIdentifierStart(), "true")

        assertPrints('1'.isJavaIdentifierStart(), "false")
        assertPrints(';'.isJavaIdentifierStart(), "false")
        assertPrints('+'.isJavaIdentifierStart(), "false")
    }

    @Sample
    fun isWhitespace() {
        assertPrints(' '.isWhitespace(), "true")
        assertPrints('\t'.isWhitespace(), "true")

        assertPrints('1'.isWhitespace(), "false")
        assertPrints('a'.isWhitespace(), "false")
    }

    @Sample
    fun isUpperCase() {
        assertPrints('A'.isUpperCase(), "true")
        assertPrints('Ψ'.isUpperCase(), "true")

        assertPrints('a'.isUpperCase(), "false")
        assertPrints('1'.isUpperCase(), "false")
        assertPrints('+'.isUpperCase(), "false")
    }

    @Sample
    fun isLowerCase() {
        assertPrints('a'.isLowerCase(), "true")
        assertPrints('λ'.isLowerCase(), "true")

        assertPrints('A'.isLowerCase(), "false")
        assertPrints('1'.isLowerCase(), "false")
        assertPrints('+'.isLowerCase(), "false")
    }

    @Sample
    fun toUpperCase() {
        assertPrints('a'.toUpperCase(), "A")
        assertPrints('ω'.toUpperCase(), "Ω")

        assertPrints('1'.toUpperCase(), "1")
        assertPrints('A'.toUpperCase(), "A")
        assertPrints('+'.toUpperCase(), "+")
    }

    @Sample
    fun toLowerCase() {
        assertPrints('A'.toLowerCase(), "a")
        assertPrints('Ω'.toLowerCase(), "ω")

        assertPrints('1'.toLowerCase(), "1")
        assertPrints('a'.toLowerCase(), "a")
        assertPrints('+'.toLowerCase(), "+")
    }


    @Sample
    fun isTitleCase() {
        assertPrints('ǅ'.isTitleCase(), "true")
        assertPrints('ǈ'.isTitleCase(), "true")
        assertPrints('ǋ'.isTitleCase(), "true")
        assertPrints('ǲ'.isTitleCase(), "true")

        assertPrints('1'.isTitleCase(), "false")
        assertPrints('A'.isTitleCase(), "false")
        assertPrints('a'.isTitleCase(), "false")
        assertPrints('+'.isTitleCase(), "false")
    }

    @Sample
    fun toTitleCase() {
        assertPrints('a'.toTitleCase(), "A")
        assertPrints('ǆ'.toTitleCase(), "ǅ")

        assertPrints('1'.toTitleCase(), "1")
        assertPrints('+'.toTitleCase(), "+")
    }

    @Sample
    fun isHighSurrogate() {
        assertPrints('\uD800'.isHighSurrogate(), "true")
        assertPrints('\uDBFF'.isHighSurrogate(), "true")

        assertPrints('A'.isHighSurrogate(), "false")
        assertPrints('a'.isHighSurrogate(), "false")
        assertPrints('1'.isHighSurrogate(), "false")
    }

    @Sample
    fun isLowSurrogate() {
        assertPrints('\uDC00'.isLowSurrogate(), "true")
        assertPrints('\uDFFF'.isLowSurrogate(), "true")

        assertPrints('A'.isLowSurrogate(), "false")
        assertPrints('a'.isLowSurrogate(), "false")
        assertPrints('1'.isLowSurrogate(), "false")
    }

}
