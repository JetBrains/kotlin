/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File
import java.lang.reflect.InvocationTargetException

private const val runFromDaemonPropertyName = "kotlin.native.tool.runFromDaemon"

internal abstract class KonanCliRunner(
        protected val toolName: String,
        private val fileOperations: FileOperations,
        private val logger: Logger,
        private val isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        private val konanHome: String,
) {
    private val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    private val daemonEntryPoint get() = "daemonMain"

    private val classpath: Set<File> by lazy {
        fileOperations.fileTree("$konanHome/konan/lib/").apply {
            include("trove4j.jar")
            include("kotlin-native-compiler-embeddable.jar")
        }.files
    }

    protected open fun transformArgs(args: List<String>) = listOf(toolName) + args

    fun run(args: List<String>) {
        check(classpath.isNotEmpty()) {
            """
                Classpath of the tool is empty: $toolName
                Probably the 'kotlin.native.home' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
        }

        System.setProperty(runFromDaemonPropertyName, "true")

        val transformedArgs = transformArgs(args)

        logger.log(
                LogLevel.INFO,
                """|Run in-process tool "$toolName"
                   |Entry point method = $mainClass.$daemonEntryPoint
                   |Classpath = ${classpath.map { it.path }.toPrettyString()}
                   |Arguments = ${args.toPrettyString()}
                   |Transformed arguments = ${if (transformedArgs == args) "same as arguments" else transformedArgs.toPrettyString()}
                """.trimMargin()
        )

        try {
            val mainClass = isolatedClassLoadersService.getClassLoader(classpath).loadClass(mainClass)
            val entryPoint = mainClass.methods
                    .singleOrNull { it.name == daemonEntryPoint } ?: error("Couldn't find daemon entry point '$daemonEntryPoint'")

            entryPoint.invoke(null, transformedArgs.toTypedArray())
        } catch (t: InvocationTargetException) {
            throw t.targetException
        }
    }

    companion object {
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
    }
}
