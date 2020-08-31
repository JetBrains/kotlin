/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl


// String Builder

private val indentFirstLineOn = setOf(null, '\n')

internal fun StringBuilder.append(string: String?, indent: Int) {
    append(string?.prependIndent(indent, indentFirstLine = indentFirstLineOn.contains(lastOrNull())))
}

internal fun StringBuilder.appendLine(string: String?, indent: Int) {
    appendLine(string?.prependIndent(indent, indentFirstLine = indentFirstLineOn.contains(lastOrNull())))
}

internal fun String.prependIndent(indent: Int, indentFirstLine: Boolean = false): String {
    val indentString = buildString { repeat(indent) { append("    ") } }

    return lineSequence()
        .mapIndexed { index, line ->
            when {
                !indentFirstLine && index == 0 -> line
                line.isBlank() -> {
                    when {
                        line.length < indentString.length -> indentString
                        else -> line
                    }
                }
                else -> indentString + line
            }
        }
        .joinToString("\n")
}

internal fun <T> StringBuilder.appendJoined(items: Iterable<T>, separator: String, block: StringBuilder.(T) -> Unit) {
    val count = items.count()
    for ((index, item) in items.withIndex()) {
        block(item)
        if (index < count - 1) {
            append(separator)
        }
    }
}
