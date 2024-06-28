/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName

internal typealias SourceText = CharSequence
internal typealias SourceLine = String

internal data class NumberedSourceLine(val number: Int, val text: SourceLine) : CharSequence by text {
    override fun toString() = text
}

internal fun SourceText.hasAnythingButComments(): Boolean =
    dropNonMeaningfulLines().firstOrNull() != null

internal fun NumberedSourceLine.getExistingPackageName(): PackageName? =
    text.substringAfter("package ", missingDelimiterValue = "")
        .trimStart()
        .takeIf(String::isNotEmpty)
        ?.let(::PackageName)

internal fun SourceText.dropNonMeaningfulLines(): Sequence<NumberedSourceLine> {
    var inMultilineComment = false

    return lineSequence()
        .mapIndexed { lineNumber, line -> NumberedSourceLine(lineNumber, line.trim()) }
        .dropWhile { (_, trimmedLine) ->
            when {
                inMultilineComment -> inMultilineComment = !trimmedLine.endsWith("*/")
                trimmedLine.startsWith("/*") -> inMultilineComment = true
                trimmedLine.isMeaningfulLine() -> return@dropWhile false
            }
            return@dropWhile true
        }
}

private fun SourceLine.isMeaningfulLine() = isNotEmpty() && !startsWith("//") && !startsWith("@file:")
