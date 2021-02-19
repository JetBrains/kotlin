/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.gradle.internal.testing.ParsedStackTrace
import org.jetbrains.kotlin.gradle.utils.appendLine

data class KotlinNativeStackTrace(
    val message: String?,
    val stackTrace: List<KotlinNativeStackTraceElement>?
) {
    fun toJvm() = ParsedStackTrace(
        message,
        stackTrace?.map { it.toJvmStackTraceElement() }
    )


    override fun toString(): String {
        return "KotlinNativeStackTrace(\nmessage=\"$message\",\nstacktrace=[\n${stackTrace?.joinToString("\n")}\n])"
    }
}

data class KotlinNativeStackTraceElement(
    val bin: String?,
    val address: String?,
    val className: String?,
    val methodName: String?,
    val signature: String?,
    val offset: Int = -1,
    val fileName: String?,
    val lineNumber: Int = -1,
    val columnNumber: Int = -1
) {
    fun toJvmStackTraceElement() = StackTraceElement(
        className ?: "<global>",
        methodName ?: "<unknown>",
        fileName,
        lineNumber
    )
}

fun parseKotlinNativeStackTraceAsJvm(stackTrace: String): ParsedStackTrace? =
    parseKotlinNativeStackTrace(stackTrace).toJvm()

fun parseKotlinNativeStackTrace(stackTrace: String): KotlinNativeStackTrace {
    val message = StringBuilder()
    var firstLines = true
    val stack = mutableListOf<KotlinNativeStackTraceElement>()

    // see examples in KotlinNativeStackTraceParserKtTest
    stackTrace.lines().forEach {
        val srcLine = it.trim()

        val bin: String?
        val address: String?
        val className: String?
        val methodName: String?
        val signature: String?
        val offset: Int
        var fileName: String? = null
        var lineNumber: Int = -1
        var columnNumber: Int = -1

        fun parsePos(fileAndPos: String) {
            val fileAndPosComponents = fileAndPos.split(":")
            fileName = fileAndPosComponents[0]
            if (fileAndPosComponents.size > 1) lineNumber = fileAndPosComponents[1].toIntOrNull() ?: -1
            if (fileAndPosComponents.size > 2) columnNumber = fileAndPosComponents[2].toIntOrNull() ?: -1
        }

        // Example with debug info:
        // at 15  test.kexe 0x0000000104902e12 kfun:f.q.n<f.q.n>(f.q.n<f.q.n>,f.q.n<f.q.n>) + 50 (/file/name.kt:23:5)
        // Without:
        // at 15  test.kexe 0x0000000104902e12 kfun:f.q.n<f.q.n>(f.q.n<f.q.n>,f.q.n<f.q.n>) + 50
        if (srcLine.startsWith("at ")) {
            firstLines = false
            val line = srcLine.removePrefix("at ")

            val offsetPos = line.indexOf('+')
            if (offsetPos > 0) {
                val withoutFileAndPos = if (line.indexOf('(', offsetPos) > 0) {
                    val fileAndPos = line.substringAfterLast("(").removeSuffix(")")
                    parsePos(fileAndPos)
                    line.substringBeforeLast("(")
                } else line

                val components = withoutFileAndPos.split(Regex("\\s+"))

                if (components.size > 5) {
                    // val number = components[0]
                    bin = components[1]
                    address = components[2]
                    var classAndMethod = components[3]
                    // val plus = components[4]
                    offset = components[5].toIntOrNull() ?: -1

                    classAndMethod = classAndMethod.removePrefix("kfun:")
                    signature = "(" + classAndMethod.substringAfterLast("(")
                    classAndMethod = classAndMethod.substringBeforeLast("(")

                    if ("." in classAndMethod) {
                        methodName = classAndMethod.substringAfterLast(".").trim().takeUnless(String::isBlank)
                        className = classAndMethod.substringBeforeLast(".").trim()
                    } else {
                        methodName = classAndMethod.trim().takeUnless(String::isBlank)
                        className = null
                    }

                    stack.add(
                        KotlinNativeStackTraceElement(
                            bin,
                            address,
                            className,
                            methodName,
                            signature,
                            offset,
                            fileName,
                            lineNumber,
                            columnNumber
                        )
                    )
                }
            }
        } else {
            if (firstLines) {
                message.appendLine(it)
            }
        }
    }

    return KotlinNativeStackTrace(
        message.toString().trim().let { if (it.isEmpty()) null else it },
        if (stack.isEmpty()) null else stack
    )
}
