/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

internal fun parseDouble(string: String): Double {
    if (string.isEmpty()) numberFormatError(string)

    var index = 0

    fun parseUnsignificants() {
        while (index <= string.lastIndex) {
            if (string[index] != ' ' && string[index] != '\n' && string[index] != '\t') break
            index++
        }
    }

    fun parseNumber(sb: StringBuilder) {
        while (index <= string.lastIndex) {
            val ch = string[index]
            if (ch !in '0'..'9') break
            sb.append(string[index])
            index++
        }
    }

    fun parseSign(): Boolean {
        if (index > string.lastIndex) return false
        val ch = string[index]
        if (ch == '+' || ch == '-') {
            index++
            return ch == '-'
        }
        return false
    }

    fun parseE(): Boolean {
        if (index > string.lastIndex) return false
        val ch = string[index]
        if (ch == 'e' || ch == 'E') {
            index++
            return true
        }
        return false
    }

    fun parseDot(): Boolean {
        if (index > string.lastIndex) return false
        val ch = string[index]
        if (ch == '.') {
            index++
            return true
        }
        return false
    }

    fun tryParseWord(word: String): Boolean {
        if (string.length - index < word.length) return false
        val originalIndex = index
        var wordIndex = 0
        while (wordIndex < word.length) {
            if (string[index] != word[wordIndex]) {
                index = originalIndex
                return false
            }
            wordIndex++
            index++
        }
        return true
    }

    parseUnsignificants()
    val isNegative = parseSign()

    if (tryParseWord("NaN")) {
        parseUnsignificants()
        if (index != string.length) numberFormatError(string)
        return if (!isNegative) Double.NaN else -Double.NaN
    }
    if (tryParseWord("Infinity")) {
        parseUnsignificants()
        if (index != string.length) numberFormatError(string)
        return if (!isNegative) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
    }

    val numberBuilder = StringBuilder()
    parseNumber(numberBuilder)
    var scale = numberBuilder.length
    if (parseDot()) parseNumber(numberBuilder)
    if (numberBuilder.isEmpty()) numberFormatError(string)
    val numberString = numberBuilder.toString()

    if (parseE()) {
        numberBuilder.clear()
        val isExponentNegative = parseSign()
        parseNumber(numberBuilder)
        if (numberBuilder.isEmpty()) numberFormatError(string)
        parseUnsignificants()
        if (index != string.length) numberFormatError(string)

        if (numberString.all { it == '0' }) {
            return if (isNegative) -0.0 else 0.0
        }

        val exponentScale = numberBuilder.toString().toIntOrNull() ?: run {
            return if (isExponentNegative) {
                if (isNegative) -0.0 else 0.0
            } else {
                if (isNegative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
            }
        }

        if (isExponentNegative) {
            scale -= exponentScale
        } else {
            scale += exponentScale
        }
    } else {
        parseUnsignificants()
        if (index != string.length) numberFormatError(string)
    }

    return numberToDouble(isNegative, scale, numberString)
}