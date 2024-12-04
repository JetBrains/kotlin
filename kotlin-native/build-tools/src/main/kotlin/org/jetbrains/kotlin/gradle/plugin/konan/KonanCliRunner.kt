/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files

private fun Collection<String>.toPrettyString(): String = buildString {
    append('[')
    if (this@toPrettyString.isNotEmpty()) append('\n')
    this@toPrettyString.forEach { append('\t').append(it.toPrettyString()).append('\n') }
    append(']')
}

private fun String.toPrettyString(): String =
        when {
            isEmpty() -> "\"\""
            any { it == '"' || it.isWhitespace() } -> '"' + escapeStringCharacters() + '"'
            else -> this
        }

private fun String.escapeStringCharacters(): String {
    val buffer = StringBuilder(length)
    escapeStringCharacters(length, "\"", true, true, buffer)
    return buffer.toString()
}

private fun String.escapeStringCharacters(
        length: Int,
        additionalChars: String?,
        escapeSlash: Boolean,
        escapeUnicode: Boolean,
        buffer: StringBuilder
): StringBuilder {
    var prev = 0.toChar()
    for (idx in 0..<length) {
        val ch = this[idx]
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
                val hexCode: CharSequence = Integer.toHexString(ch.code).uppercase()
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
    return buffer
}

private fun isPrintableUnicode(c: Char): Boolean {
    val t = Character.getType(c)
    return t != Character.UNASSIGNED.toInt() &&
            t != Character.LINE_SEPARATOR.toInt() &&
            t != Character.PARAGRAPH_SEPARATOR.toInt() &&
            t != Character.CONTROL.toInt() &&
            t != Character.FORMAT.toInt() &&
            t != Character.PRIVATE_USE.toInt() &&
            t != Character.SURROGATE.toInt()
}

private const val runFromDaemonPropertyName = "kotlin.native.tool.runFromDaemon"

internal fun ClassLoader.runKonanTool(
        toolName: String,
        args: List<String>,
        useArgFile: Boolean,
) {
    val mainClass = "org.jetbrains.kotlin.cli.utilities.MainKt"
    val daemonEntryPoint = "daemonMain"

    System.setProperty(runFromDaemonPropertyName, "true")

    val transformedArgs = if (useArgFile) {
        val argFile = Files.createTempFile(/* prefix = */ "konancArgs", /* suffix = */ ".lst").toFile().apply { deleteOnExit() }
        argFile.printWriter().use { w ->
            for (arg in args) {
                val escapedArg = arg
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        listOf("@${argFile.absolutePath}")
    } else {
        args
    }

    Logging.getLogger("KonanCliRunner").log(
            LogLevel.INFO,
            """|Run in-process tool "$toolName"
                   |Arguments = ${args.toPrettyString()}
                   |Transformed arguments = ${if (transformedArgs == args) "same as arguments" else transformedArgs.toPrettyString()}
                """.trimMargin()
    )

    try {
        val mainClass = loadClass(mainClass)
        val entryPoint = mainClass.methods
                .singleOrNull { it.name == daemonEntryPoint } ?: error("Couldn't find daemon entry point '$daemonEntryPoint'")

        entryPoint.invoke(null, (listOf(toolName) + transformedArgs).toTypedArray())
    } catch (t: InvocationTargetException) {
        throw t.targetException
    }
}
