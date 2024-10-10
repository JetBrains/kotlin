/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import kotlin.text.toUpperCase

internal fun lowerCamelCaseName(vararg nameParts: String?): String {
    val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
    return nonEmptyParts.drop(1).joinToString(
        separator = "",
        prefix = nonEmptyParts.firstOrNull().orEmpty(),
        transform = String::capitalizeAsciiOnly
    )
}

internal fun dashSeparatedToUpperCamelCase(name: String): String {
    return name.split("-").joinToString("") { it.capitalize() }
}

internal fun dashSeparatedName(nameParts: Iterable<String?>) = dashSeparatedName(*nameParts.toList().toTypedArray())

internal fun dashSeparatedName(vararg nameParts: String?): String {
    val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
    return nonEmptyParts.joinToString(separator = "-")
}

internal fun String.decamelize(): String {
    return replace(upperCaseRegex) {
        val (first) = it.destructured
        "-${first.toLowerCaseAsciiOnly()}"
    }
}

private val upperCaseRegex = "([A-Z])".toRegex()

private val invalidTaskNameCharacters = "[/\\\\:<>\"?*|]".toRegex()

/**
 * Replaces characters which are not allowed in Gradle task names (/, \, :, <, >, ", ?, *, |) with '_'
 */
internal fun String.asValidTaskName() = replace(invalidTaskNameCharacters, "_")

private val ANSI_COLOR_REGEX = "\\x1b\\[[0-9;]*m".toRegex()

internal fun String.clearAnsiColor() =
    replace(ANSI_COLOR_REGEX, "")

// Copy of stdlib's appendLine which is only available since 1.4. Can be removed as soon as this code is compiled with API >= 1.4.
internal fun Appendable.appendLine(value: Any?): Appendable =
    append(value.toString()).appendLine()

internal fun Appendable.appendLine(): Appendable =
    append('\n')

internal fun String.removingTrailingNewline(): String = this.dropLastWhile { it == '\n' }

// copied from IJ
@OptIn(ExperimentalStdlibApi::class)
internal fun escapeStringCharacters(
    str: String,
    length: Int = str.length,
    buffer: StringBuilder = StringBuilder(length),
    additionalChars: String? = "\"",
    escapeSlash: Boolean = true,
    escapeUnicode: Boolean = true,
): String {
    var prev = 0.toChar()
    for (idx in 0 until length) {
        val ch = str.get(idx)
        when (ch) {
            '\b' -> buffer.append("\\b")
            '\t' -> buffer.append("\\t")
            '\n' -> buffer.append("\\n")
            '\u000c' -> buffer.append("\\f")
            '\r' -> buffer.append("\\r")
            else -> if (escapeSlash && ch == '\\') {
                buffer.append("\\\\")
            } else if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
                buffer.append("\\").append(ch)
            } else if (escapeUnicode && !isPrintableUnicode(ch)) {
                val hexCode: CharSequence = Integer.toHexString(ch.code).toUpperCase()
                buffer.append("\\u")
                var paddingCount = 4 - hexCode.length
                while (paddingCount-- > 0) {
                    buffer.append(0)
                }
                buffer.append(hexCode)
            } else {
                buffer.append(ch)
            }
        }
        prev = ch
    }
    return buffer.toString()
}

// copied from IJ
private fun isPrintableUnicode(c: Char): Boolean {
    val t = Character.getType(c).toByte()
    val block = Character.UnicodeBlock.of(c)
    return t != Character.UNASSIGNED &&
            t != Character.LINE_SEPARATOR &&
            t != Character.PARAGRAPH_SEPARATOR &&
            t != Character.CONTROL &&
            t != Character.FORMAT &&
            t != Character.PRIVATE_USE &&
            t != Character.SURROGATE &&
            block != Character.UnicodeBlock.VARIATION_SELECTORS &&
            block != Character.UnicodeBlock.VARIATION_SELECTORS_SUPPLEMENT
}