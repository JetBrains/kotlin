/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import org.jetbrains.kotlin.gradle.internal.testing.ParsedStackTrace
import org.jetbrains.kotlin.gradle.utils.appendLine

data class NodeJsStackTrace(
    val message: String?,
    val stackTrace: List<NodeJsStackTraceElement>?
) {
    fun toJvm() = ParsedStackTrace(
        message,
        stackTrace?.map { it.toJvmStackTraceElement() }
    )


    override fun toString(): String {
        return "NodeJsStackTrace(\nmessage=\"$message\",\nstacktrace=[\n${stackTrace?.joinToString("\n")}\n])"
    }
}

data class NodeJsStackTraceElement(
    val className: String? = null,
    val methodName: String? = null,
    val fileName: String? = null,
    val lineNumber: Int = -1,
    val colNumber: Int = -1
) {
    fun toJvmStackTraceElement() = StackTraceElement(
        className ?: "<global>",
        methodName ?: "<unknown>",
        fileName,
        lineNumber
    )
}

fun parseNodeJsStackTraceAsJvm(stackTrace: String): ParsedStackTrace? =
    parseNodeJsStackTrace(stackTrace).toJvm()

fun parseNodeJsStackTrace(stackTrace: String): NodeJsStackTrace {
    val message = StringBuilder()
    var firstLines = true
    val stack = mutableListOf<NodeJsStackTraceElement>()

    // see examples at NodeJsStackTraceParserKtTest
    stackTrace.lines().forEach {
        val srcLine = it.trim()

        var className: String? = null
        var methodName: String? = null
        var fileName: String? = null
        var lineNumber: Int? = null
        var colNumber: Int? = null

        fun parsePos(fileAndPos: String) {
            val components = fileAndPos.split(":").toMutableList()
            if (components.size > 2) {
                colNumber = components.removeAt(components.size - 1).toIntOrNull()
            }
            if (components.size > 1) {
                lineNumber = components.removeAt(components.size - 1).toIntOrNull()
            }
            fileName = components.joinToString(":")
        }

        if (srcLine.startsWith("at ")) {
            firstLines = false
            val line = srcLine.removePrefix("at ")

            val classAndMethod: String? = if (line.endsWith(")") && "(" in line) {
                val fileAndPos = line.substringAfterLast("(").removeSuffix(")")
                parsePos(fileAndPos)
                line.substringBeforeLast("(")
            } else if (line.contains(":")) {
                parsePos(line)
                null
            } else line

            if (classAndMethod != null) {
                if ("." in classAndMethod) {
                    methodName = filterMethodName(classAndMethod.substringAfterLast(".")).trim()
                    className = classAndMethod.substringBeforeLast(".").trim()
                } else {
                    methodName = classAndMethod.trim()
                }

                if (methodName.endsWith("]") && "[as " in methodName) {
                    methodName = methodName.substringAfterLast("[as ").removeSuffix("]").trim()
                }

                methodName = filterMethodName(methodName)

                if (methodName.isBlank()) methodName = null

                if (methodName != null) {
                    if (methodName.endsWith("_init")) {
                        className = methodName.removeSuffix("_init")
                        methodName = "init"
                    }
                }

                if (className != null) {
                    className = filterClassName(className)
                }
            }

            stack.add(
                NodeJsStackTraceElement(
                    className,
                    methodName,
                    fileName,
                    lineNumber ?: -1,
                    colNumber ?: -1
                )
            )
        } else {
            if (firstLines) {
                message.appendLine(it)
            }
        }
    }

    return NodeJsStackTrace(
        message.toString().trim().let { if (it.isEmpty()) null else it },
        if (stack.isEmpty()) null else stack
    )
}

fun filterClassName(className: String): String =
    className.substringAfterLast('$')

private fun filterMethodName(name: String): String =
    if ("_" in name) {
        val suffix = name.substringAfterLast("_")
        if (suffix.endsWith("$") || suffix.toIntOrNull() != null) name.substringBeforeLast("_")
        else name
    } else name
