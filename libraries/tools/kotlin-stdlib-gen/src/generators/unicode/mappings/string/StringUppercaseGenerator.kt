/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.string

import generators.unicode.SpecialCasingLine
import generators.unicode.UnicodeDataLine
import generators.unicode.writeHeader
import templates.KotlinTarget
import java.io.File
import java.io.FileWriter

internal class StringUppercaseGenerator(
    private val outputFile: File,
    unicodeDataLines: List<UnicodeDataLine>,
    private val target: KotlinTarget
) : StringCasingGenerator(unicodeDataLines) {

    override fun SpecialCasingLine.mapping(): List<String> = uppercaseMapping

    override fun UnicodeDataLine.mapping(): String = uppercaseMapping

    fun generate() {
        check(contextDependentMappings.isEmpty()) {
            "The locale-agnostic conditional mappings $contextDependentMappings are not handled."
        }

        FileWriter(outputFile).use { writer ->
            writer.writeHeader(outputFile, "kotlin.text")
            writer.appendLine()
            writer.appendLine(codePointAt())
            writer.appendLine()
            writer.appendLine(charCount())
            writer.appendLine()
            writer.appendLine(appendCodePoint())
            writer.appendLine()
            writer.appendLine(uppercaseImpl())
        }
    }

    private fun charCount(): String = """
        internal fun Int.charCount(): Int = if (this > Char.MAX_VALUE.code) 2 else 1 
    """.trimIndent()

    private fun codePointAt(): String = """
        internal fun String.codePointAt(index: Int): Int {
            val high = this[index]
            if (high.isHighSurrogate() && index + 1 < this.length) {
                val low = this[index + 1]
                if (low.isLowSurrogate()) {
                    return Char.toCodePoint(high, low)
                }
            }
            return high.code
        }
    """.trimIndent().prependOptInExperimentalNativeApi(target)

    private fun appendCodePoint(): String = """
        internal fun StringBuilder.appendCodePoint(codePoint: Int) {
            if (codePoint <= Char.MAX_VALUE.code) {
                append(codePoint.toChar())
            } else {
                append(Char.MIN_HIGH_SURROGATE + ((codePoint - 0x10000) shr 10))
                append(Char.MIN_LOW_SURROGATE + (codePoint and 0x3ff))
            }
        }
    """.trimIndent()

    private fun uppercaseImpl(): String = """
        internal fun String.uppercaseImpl(): String {
            var unchangedIndex = 0
            while (unchangedIndex < this.length) {
                val codePoint = codePointAt(unchangedIndex)
                if (this[unchangedIndex].oneToManyUppercase() != null || codePoint.uppercaseCodePoint() != codePoint) {
                    break
                }
                unchangedIndex += codePoint.charCount()
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
                val uppercaseCodePoint = codePoint.uppercaseCodePoint()
                sb.appendCodePoint(uppercaseCodePoint)
                index += codePoint.charCount()
            }

            return sb.toString()
        }
    """.trimIndent()
}

internal fun String.prependOptInExperimentalNativeApi(target: KotlinTarget): String {
    return if (target == KotlinTarget.Native) {
        "@OptIn(kotlin.experimental.ExperimentalNativeApi::class)\n$this"
    } else {
        this
    }
}