/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.text.unicode.*

// \p{cased} (\p{case-ignorable})* Sigma !( (\p{case-ignorable})* \p{cased} )
// The regular-expression operator * is "possessive", consuming as many characters as possible, with no backup.
// This is significant in the case of Final_Sigma, because the sets of case-ignorable and cased characters are not disjoint.
@OptIn(ExperimentalUnicodeApi::class)
private fun String.isFinalSigmaAt(index: Int): Boolean {
    if (this[index] == '\u03A3' && index > 0) {
        var i = index
        var codePoint: CodePoint = CodePoint.MIN_VALUE
        while (i > 0) {
            codePoint = codePointBefore(i)
            if (isCaseIgnorable(codePoint.code)) {
                i -= codePoint.size
            } else {
                break
            }
        }
        if (i > 0 && isCased(codePoint.code)) {
            var j = index + 1
            while (j < length) {
                codePoint = codePointAt(j)
                if (isCaseIgnorable(codePoint.code)) {
                    j += codePoint.size
                } else {
                    break
                }
            }
            if (j >= length || !isCased(codePoint.code)) {
                return true
            }
        }
    }
    return false
}

@OptIn(ExperimentalUnicodeApi::class)
internal fun String.lowercaseImpl(): String {
    var unchangedIndex = 0
    while (unchangedIndex < this.length) {
        val codePoint = codePointAt(unchangedIndex)
        if (lowercaseCodePoint(codePoint.code) != codePoint.code) { // '\u0130' and '\u03A3' have lowercase corresponding mapping in UnicodeData.txt, no need to check them separately
            break
        }
        unchangedIndex += codePoint.size
    }
    if (unchangedIndex == this.length) {
        return this
    }

    val sb = StringBuilder(this.length)
    sb.appendRange(this, 0, unchangedIndex)

    var index = unchangedIndex

    while (index < this.length) {
        if (this[index] == '\u0130') {
            sb.append("\u0069\u0307")
            index++
            continue
        }
        if (isFinalSigmaAt(index)) {
            sb.append('\u03C2')
            index++
            continue
        }
        val codePoint = codePointAt(index)
        val lowercaseCodePoint = lowercaseCodePoint(codePoint.code).toCodePoint()
        sb.appendCodePoint(lowercaseCodePoint)
        index += codePoint.size
    }

    return sb.toString()
}

@OptIn(ExperimentalUnicodeApi::class)
internal fun String.uppercaseImpl(): String {
    var unchangedIndex = 0
    while (unchangedIndex < this.length) {
        val codePoint = codePointAt(unchangedIndex)
        if (this[unchangedIndex].oneToManyUppercase() != null || uppercaseCodePoint(codePoint.code) != codePoint.code) {
            break
        }
        unchangedIndex += codePoint.size
    }
    if (unchangedIndex == this.length) {
        return this
    }

    val sb = StringBuilder(this.length)
    sb.appendRange(this, 0, unchangedIndex)

    var index = unchangedIndex

    while (index < this.length) {
        val specialCasing = this[index].oneToManyUppercase()
        if (specialCasing != null) {
            sb.append(specialCasing)
            index++
            continue
        }
        val codePoint = codePointAt(index)
        val uppercaseCodePoint = uppercaseCodePoint(codePoint.code).toCodePoint()
        sb.appendCodePoint(uppercaseCodePoint)
        index += codePoint.size
    }

    return sb.toString()
}
